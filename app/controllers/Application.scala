package controllers

import javax.inject.Inject

import areas.{AreaComparison, BoundingBox}
import com.esri.core.geometry.Point
import graph.{Area, GraphNode, GraphService}
import play.api.{Configuration, Logger}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService) extends Controller with BoundingBox with AreaComparison {

  private val maxBoxApiKey = configuration.getString("mapbox.api.key").get

  def index(qo: Option[String]) = Action.async { request =>
    val nodes = nodesFor(qo.map(parseComponents).getOrElse(Seq()))

    val lastNode: GraphNode = nodes.last
    val children = lastNode.children.map(_.area)
    val areaBoundingBox = boundingBoxFor(lastNode.area.points)

    val ids = nodes.foldLeft(Seq[Seq[Long]]()) { (i, n) =>
      val next = i.lastOption.getOrElse(Seq.empty) :+ n.area.id
      i :+ next
    }
    val crumbs = nodes.map(n => n.area.name.getOrElse(n.area.id.toString)).zip(ids)

    val osmUrl = lastNode.area.osmId.map { osmId =>

      val osmTypes = Set("node", "way", "relation")

      val osmType = osmId.takeRight(1).toLowerCase()

      "https://www.openstreetmap.org/" + osmTypes.find(t => t.startsWith(osmType)).getOrElse(osmType) + "/" + osmId.dropRight(1)
    }
    Logger.info("OSM: " + osmUrl)

    Future.successful(Ok(views.html.index(lastNode.area, crumbs, children, osmUrl, maxBoxApiKey, areaBoundingBox)))
  }

  def show(qo: Option[String]) = Action.async { request =>
    val nodes = nodesFor(qo.map(parseComponents).getOrElse(Seq()))

    implicit val pw = Json.writes[graph.Point]
    implicit val aw = Json.writes[Area]
    Future.successful(Ok(Json.toJson(nodes.map(_.area))))
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
        gn.area.name.map(n => "name" -> Json.toJson(n)),
        gn.area.osmId.map(o => "osmId" -> Json.toJson(o))
      ).flatten.toMap
      Json.toJson(fields)
    }

    val json = containing.map(g => g.map(i => toJson(i)))

    Future.successful(Ok(Json.toJson(json)))
  }

  private def renderAreaStack(stack: Seq[GraphNode]) = {
    stack.map(a => a.area.name.getOrElse("?")).mkString(" / ")
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