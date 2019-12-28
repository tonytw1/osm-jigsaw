package controllers

import javax.inject.Inject

import areas.BoundingBox
import com.esri.core.geometry.Point
import graph.GraphService
import model._
import naming.NaiveNamingService
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}
import tags.{EntityNameTags, TagService}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService, val tagService: TagService,
                            naiveNamingService: NaiveNamingService) extends Controller with BoundingBox with OsmIdParsing with EntityNameTags {

  def tags(osmId: String) = Action.async { request =>
    val id = toOsmId(osmId)
    val tags = graphService.tagsFor(id).getOrElse(Map())
    Future.successful(Ok(Json.toJson(tags)))
  }

  // Given a location return all of sequences of overlapping areas which enclose it
  def reverse(lat: Double, lon: Double) = Action.async { request =>
    val requestedLanguage = request.acceptLanguages.headOption.map(l => l.locale.getLanguage)
    Logger.info("Accept language: " + requestedLanguage)

    val pt = new Point(lat, lon)

    val paths = graphService.pathsDownTo(pt).map(_.map(i => renderNode(i, requestedLanguage)))

    Future.successful(Ok(Json.toJson(paths)))
  }

  // Given a location return a name for this location
  def name(lat: Double, lon: Double) = Action.async { request =>
    val requestedLanguage = request.acceptLanguages.headOption.map(l => l.locale.getLanguage)
    Logger.info("Accept language: " + requestedLanguage)

    val pt = new Point(lat, lon)

    val paths = graphService.pathsDownTo(pt)

    val pathInformationNeededToInferPlaceName = paths.map { path =>
      path.map { node =>
        (node.area.osmIds, node.area.area)
      }
    }

    val name = naiveNamingService.nameFor(pathInformationNeededToInferPlaceName, requestedLanguage)

    Future.successful(Ok(Json.toJson(name)))
  }

  private def parseComponents(q: String): Seq[Long] = {
    q.split("/").toSeq.filter(_.nonEmpty).map(_.toLong)
  }

  private def renderNode(node: GraphNode, requestedLanguage: Option[String]  = None): JsValue = {
    val entities = node.area.osmIds.map { osmId =>
      val osmIdString = osmId.id.toString + osmId.`type`.toString
      val name = tagService.nameForOsmId(osmId, requestedLanguage).getOrElse(node.area.id.toString)
      OutputEntity(osmIdString, name)
    }

    val outputNode = OutputNode(node.area.id, entities, entities.size, node.area.area)

    implicit val ew = Json.writes[OutputEntity]
    implicit val nw = Json.writes[OutputNode]
    Json.toJson(outputNode)
  }

}