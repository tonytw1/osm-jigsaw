package controllers

import javax.inject.Inject

import areas.{AreaComparison, BoundingBox}
import com.esri.core.geometry.Point
import graph.{GraphNode, GraphService, OsmId}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import tags.TagService

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService, tagService: TagService) extends Controller with BoundingBox with AreaComparison {

  def show(qo: Option[String]) = Action.async { request =>
    val nodes = nodesFor(qo.map(parseComponents).getOrElse(Seq()))
    Future.successful(Ok(Json.toJson(nodes.map(n => renderNode(n)))))
  }

  def points(q: String) = Action.async { request =>
    val node = nodesFor(parseComponents(q)).last

    val points = node.area.points

    implicit val pw = Json.writes[graph.Point]
    Future.successful(Ok(Json.toJson(points)))
  }

  def tags(osmId: String) = Action.async { request =>
    val id = OsmId(osmId.dropRight(1).toLong, osmId.takeRight(1).charAt(0)) // TODO duplication
    val tags = graphService.tagsFor(id).getOrElse(Seq()).toMap

    Future.successful(Ok(Json.toJson(tags)))
  }

  def reverse(lat: Double, lon: Double) = Action.async { request =>

    def nodesContaining(pt: Point, node: GraphNode, stack: Seq[GraphNode]): Seq[Seq[GraphNode]] = {
      val matchingChildren = node.children.filter { c =>
        areaContainsPoint(c, pt)
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

    val jsons = containing.map(g => g.map(i => renderNode(i)))

    Future.successful(Ok(Json.toJson(jsons)))
  }

  private def parseComponents(q: String): Seq[Long] = {
    q.split("/").toSeq.filter(_.nonEmpty).map(_.toLong)
  }

  private def nodesFor(components: Seq[Long]): mutable.Seq[GraphNode] = {
    def nodeIdentifier(node: GraphNode): Long = {
      node.area.id
    }

    val nodes = mutable.ListBuffer[GraphNode]()
    var show = graphService.head
    nodes.+=(show)

    val queue = new mutable.Queue() ++ components

    while (queue.nonEmpty) {
      val next = queue.dequeue()
      val children = show.children

      val found = children.find { c =>
        nodeIdentifier(c) == next
      }

      found.map { f =>
        nodes.+=(f)
        show = f
      }
    }

    nodes
  }

  /*
  private def renderArea(area: Area): JsValue = {
    val name = area.osmId.flatMap { osmId =>
      tagService.tagsFor(osmId).flatMap { tags =>
        tags.find(t => t._1 == "name").map { t =>
          t._2
        }
      }
    }.getOrElse(area.id.toString)

     Json.toJson(Seq(
      Some("id" -> Json.toJson(area.id)),
      area.osmId.map(o => "osmId" -> Json.toJson(o)),
      Some("name" -> Json.toJson(name))
    ).flatten.toMap)
  }
  */

  private def renderNode(node: GraphNode): JsValue = {

    val name = node.area.osmIds.headOption.flatMap { osmId =>
      tagService.tagsFor(osmId).flatMap { tags =>
        tags.find(t => t._1 == "name").map { t =>
          t._2
        }
      }
    }.getOrElse(node.area.id.toString)

    Json.toJson(Seq(
      Some("id" -> Json.toJson(node.area.id)),
      Some("osmIds" -> Json.toJson(node.area.osmIds.map(i => i.id + i.`type`))),
      Some("name" -> Json.toJson(name)),
      Some("children" -> Json.toJson(node.children.size))
    ).flatten.toMap)
  }

}