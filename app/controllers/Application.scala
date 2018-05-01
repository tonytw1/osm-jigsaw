package controllers

import java.io.BufferedInputStream
import java.net.URL
import javax.inject.Inject

import graph.{Area, GraphReader}
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration) extends Controller {

  var head: Area = Area()

  {
    val file = new URL(configuration.getString("graph.url").get)
    Logger.info("Loading graph from: " + file)
    head = new GraphReader().loadGraph(new BufferedInputStream(file.openStream()))
  }

  def ping(qo: Option[String]) = Action.async { request =>

    def areaIdentifier(area: Area): String = {
      area.id.get
    }

    val components = qo.getOrElse("").split("/").toSeq
    val areas = mutable.ListBuffer[Area]()
    val queue = new mutable.Queue() ++ components

    var show = head

    while(queue.nonEmpty) {
      val next = queue.dequeue()
      show = show.children.find(a => areaIdentifier(a).contains(next)).get
      areas.+=(show)
    }

    Future.successful(Ok(views.html.index(areas, show)))
  }

}