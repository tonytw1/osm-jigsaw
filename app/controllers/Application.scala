
package controllers

import outputarea.{OutputArea, OutputPoint}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class Application extends Controller {

  def ping = Action.async { request =>
    val area = new OutputArea(name = Some("protobuf class work"))
    implicit val opw = Json.writes[OutputPoint]
    implicit val oaw = Json.writes[OutputArea]
    Future.successful(Ok(Json.toJson(area)))
  }

}