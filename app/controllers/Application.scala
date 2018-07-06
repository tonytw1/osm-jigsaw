package controllers

import javax.inject.Inject

import areas.{AreaComparison, BoundingBox}
import com.esri.core.geometry.Point
import graph.{Area, GraphNode, GraphService}
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, Controller}
import tags.TagService

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService, tagService: TagService) extends Controller with BoundingBox with AreaComparison {

  def show(qo: Option[String]) = Action.async { request =>
    val nodes = nodesFor(qo.map(parseComponents).getOrElse(Seq()))
    Future.successful(Ok(Json.toJson(nodes.map(n => renderArea(n.area)))))
  }

  def area(q: String) = Action.async { request =>
    val area = nodesFor(parseComponents(q)).last.area
    Future.successful(Ok(renderArea(area)))
  }

  def points(q: String) = Action.async { request =>
    val area = nodesFor(parseComponents(q)).last.area

    val points = (area.latitudes zip area.longitudes).map(ll => graph.Point(ll._1, ll._2))

    implicit val pw = Json.writes[graph.Point]
    Future.successful(Ok(Json.toJson(points)))
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
    val containing: Seq[Seq[GraphNode]] = nodesContaining(pt, graphService.head, Seq())

    val jsons = containing.map(g => g.map(i => renderArea(i.area)))

    Future.successful(Ok(Json.toJson(jsons)))
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

  private def renderArea(area: Area) = {
    implicit val pw = Json.writes[graph.Point]
    implicit val aw = Json.writes[Area]
    val areaJson = Json.toJson(area).as[JsObject]

    val name = area.osmId.flatMap { osmId =>
      tagService.tagsFor(osmId).flatMap { tags =>
        tags.find(t => t._1 == "name").map { t =>
          t._2
        }
      }
    }.getOrElse(area.id.toString)

    areaJson + ("name" -> Json.toJson(name)) - "points"
  }

}