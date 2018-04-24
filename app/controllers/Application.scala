package controllers

import java.io.{BufferedInputStream, File, FileInputStream}

import outputarea.{OutputArea, OutputPoint}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.collection.mutable
import scala.concurrent.Future

class Application extends Controller {

  val areas = mutable.ListBuffer[OutputArea]()

  {
    val file = new File("great-britain-latest.graph.pbf")
    val is = new BufferedInputStream(new FileInputStream(file))

    var ok = true
    while (ok) {
      val area = OutputArea.parseDelimitedFrom(is)
      area.map(a => areas += a)
      ok = area.nonEmpty
    }
  }

  def ping = Action.async { request =>
    implicit val opw = Json.writes[OutputPoint]
    implicit val oaw = Json.writes[OutputArea]
    Future.successful(Ok(Json.toJson(areas.headOption)))
  }

}