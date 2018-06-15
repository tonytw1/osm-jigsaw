package controllers

import javax.inject.Inject

import com.esri.core.geometry.{OperatorContains, Point, SpatialReference}
import graph.{Area, GraphService}
import play.api.libs.json.Json
import play.api.{Configuration, Logger}
import play.api.mvc.{Action, Controller}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService) extends Controller {

  val sr = SpatialReference.create(1)

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

    Logger.info("Areas: " + areas.map(a => a.name).mkString(" / "))
    val lastArea = areas.last
    Logger.info("Last area: " + lastArea.name)

    Future.successful(Ok(views.html.index(areas, show)))
  }

  def reverse(lat: Double, lon: Double) = Action.async { request =>

    def areasContaining(pt: Point, area: Area, stack: Seq[Area]): Seq[Seq[Area]] = {
      Logger.info("Checking area: " + renderAreaStack(stack) + " / " + area.name.getOrElse(""))

      val matchingChildren = area.children.toSeq.filter { c =>
        c.polygon.map { p =>
          OperatorContains.local().execute(p, pt, sr, null)
        }.getOrElse {
          Logger.warn("Area has no polygon: " + c.name)
          false
        }
      }

      if (matchingChildren.nonEmpty) {
        matchingChildren.flatMap { m =>
          areasContaining(pt, m, stack :+ area)
        }
      } else {
        Seq(stack :+ area)
      }
    }

    val pt = new Point(lat, lon)
    val output = areasContaining(pt, graphService.head.children.head, Seq()).map { a =>
      renderAreaStack(a)
    }

    Future.successful(Ok(Json.toJson(output)))
  }

  private def renderAreaStack(stack: Seq[Area]) = {
    stack.map(a => a.name.getOrElse("?")).mkString(" / ")
  }

  private def areaContainsPoint(area: Area, pt: Point) = {
    OperatorContains.local().execute(area.polygon.get, pt, sr, null)  // TODO naked get
  }

}