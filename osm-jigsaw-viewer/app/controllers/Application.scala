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
      val areas = Json.parse(r.body).as[Seq[GraphNode]]

      val lastArea = areas.last

      ws.url((apiUrl + "/points").addParam("q", q)).get.flatMap { psr =>
        implicit val pr = Json.reads[Point]
        val points = Json.parse(psr.body).as[Seq[Point]]
        val areaBoundingBox = boundingBoxFor(points)

        val crumbs = areasToCrumbs(areas)

        val osmUrl = lastArea.entities.headOption.map { e =>
          val osmId = e.osmId
          val osmTypes = Set("node", "way", "relation")
          val osmType = osmId.takeRight(1).toLowerCase()
          "https://www.openstreetmap.org/" + osmTypes.find(t => t.startsWith(osmType)).getOrElse(osmType) + "/" + osmId.dropRight(1)
        }

        val tags = lastArea.entities.headOption.map { e =>
          val osmId = e.osmId
          ws.url((apiUrl + "/tags").addParam("osm_id", osmId)).get.map { r =>
            r.body
          }
        }.getOrElse(Future.successful(""))

        tags.map { ts =>
          Ok(views.html.index(lastArea, crumbs, osmUrl, maxBoxApiKey, areaBoundingBox, ts))
        }
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

  private def areasToCrumbs(nodes: Seq[GraphNode]) = {
    val ids = nodes.foldLeft(Seq[Seq[Long]]()) { (i, a) =>
      val next = i.lastOption.getOrElse(Seq.empty) :+ a.id
      i :+ next
    }
    val crumbs = nodes.map(a => a.entities.headOption.map(e => e.name).getOrElse(a.id.toString)).zip(ids) // TODO
    crumbs
  }

}