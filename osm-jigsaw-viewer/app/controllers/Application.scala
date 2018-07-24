package controllers

import javax.inject.Inject

import areas.BoundingBox
import com.netaporter.uri.dsl._
import graph._
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
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
        Logger.info("lN: " + ln.entities.size)
        ln.entities.map { e =>
          val osmId = e.osmId
          val osmTypes = Set("node", "way", "relation")
          val osmType = osmId.takeRight(1).toLowerCase()
          "https://www.openstreetmap.org/" + osmTypes.find(t => t.startsWith(osmType)).getOrElse(osmType) + "/" + osmId.dropRight(1)
        }
      }

      val eventualTags = lastNode.flatMap { ln =>
        ln.entities.headOption.map { e =>
          val osmId = e.osmId
          ws.url((apiUrl + "/tags").addParam("osm_id", osmId)).get.map { r =>
            r.body
          }
        }
      }.getOrElse(Future.successful(""))

      for {
        areaBoundingBox <- eventualAreaBoundingBox
        tags <- eventualTags
      } yield {
        Ok(views.html.index(lastNode, crumbs, osmUrls, maxBoxApiKey, areaBoundingBox, tags))
      }
    }
  }

  def click(lat: Double, lon: Double) = Action.async { request =>
    val url = (apiUrl + "/reverse").addParam("lat", lat).addParam("lon", lon)
    ws.url(url).get.map { r =>
      implicit val er = Json.reads[Entity]
      implicit val gnr = Json.reads[GraphNode]
      val results = Json.parse(r.body).as[Seq[Seq[GraphNode]]]

      val asCrumbs: Seq[Seq[(String, Seq[Long])]] = results.map { as =>
        areasToCrumbs(as)
      }

      Ok(views.html.click(asCrumbs))
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