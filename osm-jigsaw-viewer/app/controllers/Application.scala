package controllers

import areas.BoundingBox
import graph._
import io.lemonlabs.uri.Url
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, ws: WSClient, cc: ControllerComponents) extends AbstractController(cc) with BoundingBox {

  private val apiUrl = configuration.get[String]("api.url")
  private val maxBoxApiKey = configuration.get[String]("mapbox.api.key")

  def index() = Action.async { request =>
    Future.successful(Ok(views.html.index(maxBoxApiKey)))
  }

  def show(q: String, lat: Double, lon: Double) = Action.async { request =>
    ws.url(Url.parse(apiUrl + "/show").addParam("q", q).addParam(
      "lat", lat.toString).addParam(
      "lon", lon.toString).toString).get.flatMap { r =>
      implicit val er = Json.reads[Entity]
      implicit val gnr = Json.reads[GraphNode]
      val graphNodes: Seq[GraphNode] = Json.parse(r.body).as[Seq[GraphNode]]

      val lastNode = graphNodes.lastOption

      val crumbs = lastNode.map { ln =>
        areasToCrumbs(graphNodes)
      }

      val eventualAreaBoundingBox = lastNode.map { _ =>
        ws.url(Url.parse(apiUrl + "/points").addParam(
          "q", q).addParam(
          "lat", lat.toString).addParam(
          "lon", lon.toString).toString).get.map { psr =>
          implicit val pr = Json.reads[Point]
          val points = Json.parse(psr.body).as[Seq[Point]]
          val b: (Double, Double, Double, Double) = boundingBoxFor(points)
          Some(b)
        }
      }.getOrElse {
        Future.successful(None)
      }

      val osmUrls = lastNode.map { ln =>
        ln.entities.map { e =>
          val osmId = e.osmId
          val osmTypes = Set("node", "way", "relation")
          val osmType = osmId.takeRight(1).toLowerCase()
          (osmId, "https://www.openstreetmap.org/" + osmTypes.find(t => t.startsWith(osmType)).getOrElse(osmType) + "/" + osmId.dropRight(1))
        }
      }

      val eventualTagsForLastNode: Future[Map[String, String]] = lastNode.flatMap { ln =>
        ln.entities.headOption.map { e =>
          val osmId = e.osmId
          ws.url(Url.parse(apiUrl + "/tags").addParam("osm_id", osmId).toString).get.map { r =>
            Json.parse(r.body).as[Map[String, JsValue]].map { i =>
              (i._1, i._2.as[String])
            }
          }
        }
      }.getOrElse(Future.successful(Map.empty))

      for {
        areaBoundingBox <- eventualAreaBoundingBox
        tags: Map[String, String] <- eventualTagsForLastNode
      } yield {
        Ok(views.html.show(lastNode, crumbs, osmUrls, maxBoxApiKey, areaBoundingBox, tags))
      }
    }
  }

  def click(lat: Double, lon: Double) = Action.async { request =>
    val reverseApiCallUrl = Url.parse(apiUrl + "/reverse").addParam("lat", lat.toString).addParam("lon", lon.toString)
    val eventualCrumbs = ws.url(reverseApiCallUrl.toString).get.map { r =>
      implicit val er = Json.reads[Entity]
      implicit val gnr = Json.reads[GraphNode]
      val crumbs = Json.parse(r.body).as[Seq[Seq[GraphNode]]].map(areasToCrumbs)
      val duration = r.headers.get("request-time").flatMap(_.headOption).map(d => d.toInt).getOrElse(0)
      (crumbs, duration)
    }

    val nameApiCallUrl = Url.parse(apiUrl + "/name").addParam("lat", lat.toString).addParam("lon", lon.toString)
    val eventualName = ws.url(nameApiCallUrl.toString).get.map { r =>
      Json.parse(r.body).as[String]
    }

    for {
      crumbs <- eventualCrumbs
      name <- eventualName
    } yield {
      Ok(views.html.click((lat, lon), crumbs._1, name, nameApiCallUrl.toString, reverseApiCallUrl.toString, crumbs._2))
    }
  }

  private def areasToCrumbs(nodes: Seq[GraphNode]): Seq[(String, Seq[Long])] = {
    val crumbIdChains = nodes.foldLeft(Seq[Seq[Long]]()) { (i, a) =>
      i :+ (i.lastOption.getOrElse(Seq.empty) :+ a.id)
    }
    val chumbLabels = nodes.map { a =>
      a.entities.map { e =>
        e.name
      }.mkString(", ")
    }

    chumbLabels.zip(crumbIdChains)
  }

}