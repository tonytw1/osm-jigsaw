package controllers

import javax.inject.Inject

import areas.BoundingBox
import com.netaporter.uri.dsl._
import graph.{Area, Point, SparseArea}
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

      implicit val pr = Json.reads[Point]
      implicit val ar = Json.reads[Area]
      val areas = Json.parse(r.body).as[Seq[Area]]

      val lastArea = areas.last
      val children = Seq() // TODO

      ws.url((apiUrl + "/points").addParam("q", q)).get.flatMap { psr =>
        implicit val pr = Json.reads[Point]
        val points = Json.parse(psr.body).as[Seq[Point]]
        val areaBoundingBox = boundingBoxFor(points)

        val crumbs = areasToCrumbs(areas)

        val osmUrl = lastArea.osmId.map { osmId =>
          val osmTypes = Set("node", "way", "relation")
          val osmType = osmId.takeRight(1).toLowerCase()
          "https://www.openstreetmap.org/" + osmTypes.find(t => t.startsWith(osmType)).getOrElse(osmType) + "/" + osmId.dropRight(1)
        }

        val tags = lastArea.osmId.map { osmId =>
          ws.url((apiUrl + "/tags").addParam("osm_id", osmId)).get.map { r =>
            r.body
          }
        }.getOrElse(Future.successful(""))

        tags.map { ts =>
          Ok(views.html.index(lastArea, crumbs, children, osmUrl, maxBoxApiKey, areaBoundingBox, ts))
        }
      }
    }
  }

  def click(lat: Double, lon: Double) = Action.async { request =>
    val url = (apiUrl + "/reverse").addParam("lat", lat).addParam("lon", lon)
    ws.url(url).get.map { r =>
      implicit val ar = Json.reads[SparseArea]
      val results = Json.parse(r.body).as[Seq[Seq[SparseArea]]]

      val asAreas: Seq[Seq[Area]] = results.map { r =>
        r.map(a => Area(a.id, a.name, a.osmId))
      }

      val asCrumbs: Seq[Seq[(String, Seq[Long])]] = asAreas.map { as =>
        areasToCrumbs(as)
      }

      Ok(views.html.click(asCrumbs))
    }
  }

  private def areasToCrumbs(areas: Seq[Area]) = {
    val ids = areas.foldLeft(Seq[Seq[Long]]()) { (i, a) =>
      val next = i.lastOption.getOrElse(Seq.empty) :+ a.id
      i :+ next
    }
    val crumbs = areas.map(a => a.name.getOrElse(a.id.toString)).zip(ids)
    crumbs
  }

}