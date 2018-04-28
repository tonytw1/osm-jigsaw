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

  def ping(qo: Option[String]) = Action.async { request =>

    def areaIdentifier(area: Area): String = {
      area.id.get
    }

    val components = qo.getOrElse("").split("/").toSeq
    val queue = new mutable.Queue() ++ components
    var show = head
    while(queue.nonEmpty) {
      val next = queue.dequeue()
      show = show.children.find(a => areaIdentifier(a).contains(next)).get
    }

    Future.successful(Ok(views.html.index(components, show)))
  }

}