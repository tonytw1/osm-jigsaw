package controllers

import areas.BoundingBox
import com.netaporter.uri.dsl._
import graph._
import javax.inject.Inject
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, ws: WSClient) extends Controller with BoundingBox {

  private val apiUrl = configuration.getString("api.url").get
  private val maxBoxApiKey = configuration.getString("mapbox.api.key").get

  def index(qo: Option[String]) = Action.async { request =>
    val q = qo.getOrElse("")

    ws.url((apiUrl + "/show").addParam("q", q)).get.flatMap { r =>
      implicit val er = Json.reads[Entity]
      implicit val gnr = Json.reads[GraphNode]
      val graphNodes = Json.parse(r.body).as[Seq[GraphNode]]

      val lastNode = graphNodes.lastOption

      val crumbs = lastNode.map { ln =>
        areasToCrumbs(graphNodes)
      }

      val eventualAreaBoundingBox = lastNode.map { _ =>
        ws.url((apiUrl + "/points").addParam("q", q)).get.map { psr =>
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

      val eventualTags: Future[Map[String, String]] = lastNode.flatMap { ln =>
        ln.entities.headOption.map { e =>
          val osmId = e.osmId
          ws.url((apiUrl + "/tags").addParam("osm_id", osmId)).get.map { r =>
            Json.parse(r.body).as[Map[String, JsValue]].map { i =>
              (i._1, i._2.as[String])
            }
          }
        }
      }.getOrElse(Future.successful(Map.empty))

      for {
        areaBoundingBox <- eventualAreaBoundingBox
        tags <- eventualTags
      } yield {
        Ok(views.html.index(lastNode, crumbs, osmUrls, maxBoxApiKey, areaBoundingBox, tags))
      }
    }
  }

  def click(lat: Double, lon: Double) = Action.async { request =>
    val reverseApiCallUrl = (apiUrl + "/reverse").addParam("lat", lat).addParam("lon", lon)
    val eventualCrumbs = ws.url(reverseApiCallUrl).get.map { r =>
      implicit val er = Json.reads[Entity]
      implicit val gnr = Json.reads[GraphNode]
      Json.parse(r.body).as[Seq[Seq[GraphNode]]].map { as =>
        areasToCrumbs(as)
      }
    }

    val nameApiCallUrl = (apiUrl + "/name").addParam("lat", lat).addParam("lon", lon)
    val eventualName = ws.url(nameApiCallUrl).get.map { r =>
      Json.parse(r.body).as[String]
    }

    for {
      crumbs <- eventualCrumbs
      name <- eventualName

    } yield {
      Ok(views.html.click((lat, lon), crumbs, name, nameApiCallUrl, reverseApiCallUrl))
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