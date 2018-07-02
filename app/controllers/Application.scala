package controllers

import javax.inject.Inject

import areas.BoundingBox
import graph.{Area, Point}
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import com.netaporter.uri.dsl._

import scala.concurrent.ExecutionContext.Implicits.global

class Application @Inject()(configuration: Configuration, ws: WSClient) extends Controller with BoundingBox {

  private val apiUrl = configuration.getString("api.url").get
  private val maxBoxApiKey = configuration.getString("mapbox.api.key").get

  def index(qo: Option[String]) = Action.async { request =>
    val url = (apiUrl + "/show").addParam("q", qo.getOrElse(""))
    Logger.info("Calling: " + url)
    ws.url(url).get.map { r =>

      implicit val pr = Json.reads[Point]
      implicit val ar = Json.reads[Area]
      val areas = Json.parse(r.body).as[Seq[Area]]

      val lastArea = areas.last
      val children = Seq() // TODO
    val areaBoundingBox = boundingBoxFor(lastArea.points)

      val ids = areas.foldLeft(Seq[Seq[Long]]()) { (i, a) =>
        val next = i.lastOption.getOrElse(Seq.empty) :+ a.id
        i :+ next
      }
      val crumbs = areas.map(a => a.name.getOrElse(a.id.toString)).zip(ids)

      val osmUrl = lastArea.osmId.map { osmId =>
        val osmTypes = Set("node", "way", "relation")
        val osmType = osmId.takeRight(1).toLowerCase()
        "https://www.openstreetmap.org/" + osmTypes.find(t => t.startsWith(osmType)).getOrElse(osmType) + "/" + osmId.dropRight(1)
      }

      Ok(views.html.index(lastArea, crumbs, children, osmUrl, apiUrl, maxBoxApiKey, areaBoundingBox))
    }
  }

}