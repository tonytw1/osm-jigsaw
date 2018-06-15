package controllers

import javax.inject.Inject

import graph.{Area, GraphService}
import play.api.Configuration
import play.api.mvc.{Action, Controller}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService) extends Controller {

  def index(qo: Option[String]) = Action.async { request =>

    def areaIdentifier(area: Area): String = {
      area.id.get
    }

    val components = qo.getOrElse("").split("/").toSeq
    val areas = mutable.ListBuffer[Area]()
    val queue = new mutable.Queue() ++ components

    var show = graphService.head

    while(queue.nonEmpty) {
      val next = queue.dequeue()
      show = show.children.find(a => areaIdentifier(a).contains(next)).get
      areas.+=(show)
    }

    Future.successful(Ok(views.html.index(areas, show)))
  }

}