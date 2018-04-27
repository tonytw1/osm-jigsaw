package controllers

import java.io.File

import graph.{Area, GraphReader}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class Application extends Controller {

  var head: Area = Area()

  {
    val file = new File("great-britain-latest.graph.pbf")
    head = new GraphReader().loadGraph(file)
  }

  def ping = Action.async { request =>
    Future.successful(Ok(head.children.map(a => a.name).flatten.mkString(", ")))
  }

}