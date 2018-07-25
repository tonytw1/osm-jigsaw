package controllers

import areas.BoundingBox
import com.esri.core.geometry.Point
import graph.GraphService
import javax.inject.Inject
import model.{GraphNode, OsmIdParsing}
import naming.NaiveNamingService
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import tags.{EntityNameTags, TagService}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService, val tagService: TagService,
                            naiveNamingService: NaiveNamingService) extends Controller with BoundingBox with OsmIdParsing with EntityNameTags {

  def show(qo: Option[String]) = Action.async { request =>
    val nodes = nodesFor(qo.map(parseComponents).getOrElse(Seq()))
    Future.successful(Ok(Json.toJson(nodes.map(n => renderNode(n)))))
  }

  def points(q: String) = Action.async { request =>
    nodesFor(parseComponents(q)).lastOption.map { node =>

      val points = node.area.points
      implicit val pw = Json.writes[model.Point]
      Future.successful(Ok(Json.toJson(points)))
    }.getOrElse {
      Future.successful(NotFound(Json.toJson("Not found")))
    }
  }

  def tags(osmId: String) = Action.async { request =>
    val id = toOsmId(osmId)
    val tags = graphService.tagsFor(id).getOrElse(Seq()).toMap

    Future.successful(Ok(Json.toJson(tags)))
  }

  def reverse(lat: Double, lon: Double) = Action.async { request =>
    val pt = new Point(lat, lon)

    val jsons = graphService.pathsDownTo(pt).map(_.map(i => renderNode(i)))

    Future.successful(Ok(Json.toJson(jsons)))
  }

  def name(lat: Double, lon: Double) = Action.async { request =>
    val pt = new Point(lat, lon)

    val name = naiveNamingService.nameFor(graphService.pathsDownTo(pt))

    Future.successful(Ok(Json.toJson(name)))
  }

  private def parseComponents(q: String): Seq[Long] = {
    q.split("/").toSeq.filter(_.nonEmpty).map(_.toLong)
  }

  private def nodesFor(components: Seq[Long]): mutable.Seq[GraphNode] = {
    def nodeIdentifier(node: GraphNode): Long = {
      node.area.id
    }

    val nodes = mutable.ListBuffer[GraphNode]()

    val queue = new mutable.Queue() ++ components

    var currentNode = graphService.head
    while (queue.nonEmpty) {
      val next = queue.dequeue()
      val children = currentNode.children

      val found = children.find { c =>
        nodeIdentifier(c) == next
      }

      found.map { f =>
        nodes.+=(f)
        currentNode = f
      }
    }

    nodes
  }

  private def renderNode(node: GraphNode): JsValue = {
    val entities = node.area.osmIds.map { osmId =>
      val osmIdString = osmId.id.toString + osmId.`type`.toString
      val name = tagService.nameForOsmId(osmId).getOrElse(node.area.id.toString)
      OutputEntity(osmIdString, name)
    }

    val outputNode = OutputNode(node.area.id, entities, entities.size)

    implicit val ew = Json.writes[OutputEntity]
    implicit val nw = Json.writes[OutputNode]

    Json.toJson(outputNode)
  }

  case class OutputEntity(osmId: String, name: String)
  case class OutputNode(id: Long, entities: Seq[OutputEntity], children: Long)

}