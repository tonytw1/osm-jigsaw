package controllers

import javax.inject.Inject

import areas.BoundingBox
import com.esri.core.geometry.{OperatorContains, Point, Polygon, SpatialReference}
import graph.{Area, GraphService}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService) extends Controller with BoundingBox {

  private val sr = SpatialReference.create(1)
  private val maxBoxApiKey = configuration.getString("mapbox.api.key").get

  def index(qo: Option[String]) = Action.async { request =>

    def areaIdentifier(area: Area): Long = {
      area.id
    }

    val components = qo.getOrElse("").split("/").toSeq.filter(_.nonEmpty).map(_.toLong)

    var show = graphService.head
    val areas = mutable.ListBuffer[Area]()
    areas.+= (show)

    val queue = new mutable.Queue() ++ components

    while(queue.nonEmpty) {
      val next = queue.dequeue()
      val children = show.children

      val found = children.find{ a =>
        areaIdentifier(a) == next
      }

      found.map { f =>
        areas.+=(f)
        show = f
      }
    }

    Logger.info("Areas: " + areas.map(a => a.name).mkString(" / "))
    val lastArea = areas.last
    Logger.info("Last area: " + lastArea.name)

    val areaBoundingBox: (Double, Double, Double, Double) = boundingBoxFor(lastArea.points)

    val childrenHash = lastArea.children.map(c => c.id).hashCode()
    Logger.info("Child hash: " + childrenHash)
    Future.successful(Ok(views.html.index(areas, show, childrenHash, maxBoxApiKey, areaBoundingBox)))
  }

  def reverse(lat: Double, lon: Double) = Action.async { request =>

    def areasContaining(pt: Point, area: Area, stack: Seq[Area]): Seq[Seq[Area]] = {
      Logger.info("Checking area: " + renderAreaStack(stack) + " / " + area.name.getOrElse(""))

      val matchingChildren = area.children.toSeq.filter { c =>
        val childPolygon = polygonForPoints(c.points)
        childPolygon.map { p =>
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
    val output = areasContaining(pt, graphService.head, Seq()).map { a =>
      renderAreaStack(a)
    }

    Future.successful(Ok(Json.toJson(output)))
  }

  private def renderAreaStack(stack: Seq[Area]) = {
    stack.map(a => a.name.getOrElse("?")).mkString(" / ")
  }

  private def polygonForPoints(points: Seq[(Double, Double)]): Option[Polygon] = {
    points.headOption.map { n =>
      val polygon = new Polygon()
      polygon.startPath(n._1, n._2)
      points.drop(1).map { on =>
        polygon.lineTo(on._1, on._2)
      }
      polygon
    }
  }

}