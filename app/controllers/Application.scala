package controllers

import java.io.{BufferedInputStream, File}
import java.net.URL

import graph.{Area, GraphReader}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class Application extends Controller {

  var head: Area = Area()

  {
    val file = new URL(" http://10.0.45.22:32680/osm/great-britain-latest.graph.pbf")
    head = new GraphReader().loadGraph(new BufferedInputStream(file.openStream()))
  }

  def ping = Action.async { request =>
    Future.successful(Ok(head.children.map(a => a.name).flatten.mkString(", ")))
  }

}