package controllers

import areas.BoundingBox
import com.esri.core.geometry.Point
import graph.GraphService
import model._
import naming.NaiveNamingService
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import tags.{EntityNameTags, TagService}

import javax.inject.Inject
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application @Inject()(graphService: GraphService, val tagService: TagService, naiveNamingService: NaiveNamingService)
  extends Controller with BoundingBox with OsmIdParsing with EntityNameTags {

  // Given a location return all of sequences of overlapping areas which enclose it
  def reverse(lat: Double, lon: Double) = Action.async { request =>
    val point = new Point(lat, lon)
    val requestedLanguage = request.acceptLanguages.headOption.map(l => l.locale.getLanguage)
    Logger.info("Accept language: " + requestedLanguage)

    graphService.pathsDownTo(point).map { paths =>
      paths.map(_.map(i => renderNode(i, point, requestedLanguage)))
    }.map { paths =>
      Ok(Json.toJson(paths))
    }
  }

  // Given a location return a name for this location
  def name(lat: Double, lon: Double) = Action.async { request =>
    val point = new Point(lat, lon)
    val requestedLanguage = request.acceptLanguages.headOption.map(l => l.locale.getLanguage)

    val eventualPaths = graphService.pathsDownTo(point)
    eventualPaths.map { paths =>
      val pathInformationNeededToInferPlaceName = paths.map { path =>
        path.map { node =>
          (node.area.osmIds, node.area.area)
        }
      }

      val name = naiveNamingService.nameFor(pathInformationNeededToInferPlaceName, point, requestedLanguage)

      Ok(Json.toJson(name))
    }
  }

  // Given the path to an area return the sequence of graph nodes for that path
  def show(q: String, lat: Double, lon: Double) = Action.async { request =>
    val components = parseComponents(q)
    val point = new Point(lat, lon)

    nodesFor(components, point).map { nodes =>
      Ok(Json.toJson(nodes.map(n => renderNode(n, point))))
    }
  }

  // Given the path to an area return the outline for that area
  def points(q: String, lat: Double, lon: Double) = Action.async { request =>
    val components = parseComponents(q)
    val point = new Point(lat, lon)

    val eventualNodesFor: Future[Seq[GraphNode]] = nodesFor(components, point)
    eventualNodesFor.map { nodes =>
      nodes.lastOption.map { node =>
        val points = node.area.points // TODO simplify outline for quick rendering
        implicit val pw = Json.writes[model.Point]
        Ok(Json.toJson(points))
      }.getOrElse {
        NotFound(Json.toJson("Not found"))
      }
    }
  }

  private def nodesFor(components: Seq[Long], point: Point): Future[Seq[GraphNode]] = {
    def nodeIdentifier(node: GraphNode): Long = {
      node.area.id
    }

    val queue = new mutable.Queue() ++ components
    val eventualMaybeHead = graphService.headOfGraphCoveringThisPoint(point: Point)
    eventualMaybeHead.map { maybeHeadNode =>
      val nodes = mutable.ListBuffer[GraphNode]()
      maybeHeadNode.foreach { headNode =>
        var currentNode = headNode
        while (queue.nonEmpty) {
          val next = queue.dequeue()
          val children = currentNode.children

          val found = children.find { c =>
            nodeIdentifier(c) == next
          }

          found.foreach { f =>
            nodes.+=(f)
            currentNode = f
          }
        }
      }
      nodes
    }
  }

  // Given an OSM id return it's tags as a map
  def tags(osmId: String, lat: Double, lon: Double) = Action.async { request =>
    val id = toOsmId(osmId)
    val point = new Point(lat, lon)

    val tags = tagService.tagsFor(id, point).getOrElse(Map())
    Future.successful(Ok(Json.toJson(tags)))
  }

  // Healthcheck end point
  def ping() = Action.async { request =>
    Future.successful(Ok(Json.toJson("ok")))
  }

  private def parseComponents(q: String): Seq[Long] = {
    q.split("/").toSeq.filter(_.nonEmpty).map(_.toLong)
  }

  private def renderNode(node: GraphNode, point: Point, requestedLanguage: Option[String] = None): JsValue = {
    val entities = node.area.osmIds.map { osmId =>
      val osmIdString = osmId.id.toString + osmId.`type`.toString
      val name = tagService.nameForOsmId(osmId, point, requestedLanguage).getOrElse(node.area.id.toString)
      OutputEntity(osmIdString, name)
    }

    val outputNode = OutputNode(node.area.id, entities, node.children.size, node.area.area)

    implicit val ew = Json.writes[OutputEntity]
    implicit val nw = Json.writes[OutputNode]
    Json.toJson(outputNode)
  }

}