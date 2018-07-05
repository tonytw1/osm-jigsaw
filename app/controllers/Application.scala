package controllers

import javax.inject.Inject

import areas.{AreaComparison, BoundingBox}
import com.esri.core.geometry.Point
import graph.{Area, GraphNode, GraphService}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService) extends Controller with BoundingBox with AreaComparison {

  def show(qo: Option[String]) = Action.async { request =>
    val nodes = nodesFor(qo.map(parseComponents).getOrElse(Seq()))

    val lastArea = nodes.last.area
    val lastAreaTags = lastArea.osmId.flatMap { osmId =>
      graphService.tagsFor(osmId)
    }
    implicit val pw = Json.writes[graph.Point]
    implicit val aw = Json.writes[Area]
    Future.successful(Ok(Json.toJson(nodes.map(_.area))))
  }

  def tags(osmId: String) = Action.async { request =>
    val tags = graphService.tagsFor(osmId).getOrElse(Seq()).toMap

    Future.successful(Ok(Json.toJson(tags)))
  }

  def reverse(lat: Double, lon: Double) = Action.async { request =>

    def nodesContaining(pt: Point, node: GraphNode, stack: Seq[GraphNode]): Seq[Seq[GraphNode]] = {
      val matchingChildren = node.children.filter { c =>
        areaContainsPoint(c.area, pt)
      }

      if (matchingChildren.nonEmpty) {
        matchingChildren.flatMap { m =>
          nodesContaining(pt, m, stack :+ node)
        }
      } else {
        Seq(stack :+ node)
      }
    }

    val pt = new Point(lat, lon)
    val containing = nodesContaining(pt, graphService.head, Seq())

    def toJson(gn: GraphNode): JsValue = {
      val fields = Seq(
        Some("id" -> Json.toJson(gn.area.id)),
        gn.area.osmId.map(n => "name" -> Json.toJson(n)), // TODO
        gn.area.osmId.map(o => "osmId" -> Json.toJson(o))
      ).flatten.toMap
      Json.toJson(fields)
    }

    val json = containing.map(g => g.map(i => toJson(i)))

    Future.successful(Ok(Json.toJson(json)))
  }

  private def parseComponents(q: String): Seq[Long] = {
    q.split("/").toSeq.filter(_.nonEmpty).map(_.toLong)
  }

  private def nodesFor(components: Seq[Long]): mutable.Seq[GraphNode] = {
    def areaIdentifier(area: Area): Long = {
      area.id
    }

    val nodes = mutable.ListBuffer[GraphNode]()
    var show = graphService.head
    nodes.+=(show)

    val queue = new mutable.Queue() ++ components

    while (queue.nonEmpty) {
      val next = queue.dequeue()
      val children = show.children

      val found = children.find { a =>
        areaIdentifier(a.area) == next
      }

      found.map { f =>
        nodes.+=(f)
        show = f
      }
    }

    nodes
  }

}