package controllers

import java.io.{BufferedInputStream, File}
import java.net.URL

import graph.{Area, GraphReader}
import play.api.mvc.{Action, Controller}

import scala.collection.mutable
import scala.concurrent.Future

class Application extends Controller {

  var head: Area = Area()

  {
    val file = new URL(" http://10.0.45.22:32680/osm/great-britain-latest.graph.pbf")
    head = new GraphReader().loadGraph(new BufferedInputStream(file.openStream()))
  }

  def ping(q: String) = Action.async { request =>
    val components = new mutable.Queue() ++ q.split("/")
    var show = head
    while(components.nonEmpty) {
      val next = components.dequeue()
      show = show.children.find(a => a.name.contains(next)).get
    }

    Future.successful(Ok(show.children.map(a => a.name).flatten.mkString(", ")))
  }

}