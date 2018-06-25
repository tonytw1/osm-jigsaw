package controllers

import javax.inject.Inject

import areas.BoundingBox
import com.esri.core.geometry.{OperatorContains, Point, Polygon, SpatialReference}
import graph.{Area, GraphNode, GraphService}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}

import scala.collection.mutable
import scala.concurrent.Future

class Application @Inject()(configuration: Configuration, graphService: GraphService) extends Controller with BoundingBox {

  private val sr = SpatialReference.create(1)
  private val maxBoxApiKey = configuration.getString("mapbox.api.key").get

  def index(qo: Option[String]) = Action.async { request =>
    val nodes = nodesFor(qo.map(parseComponents).getOrElse(Seq()))

    val lastNode: GraphNode = nodes.last
    val children = lastNode.children.map(_.area).toSeq
    val areaBoundingBox = boundingBoxFor(lastNode.area.points)

    Future.successful(Ok(views.html.index(nodes.map(_.area), lastNode.area, children, maxBoxApiKey, areaBoundingBox)))
  }

  def show(qo: Option[String]) = Action.async { request =>
    val nodes = nodesFor(qo.map(parseComponents).getOrElse(Seq()))

    implicit val pw = Json.writes[graph.Point]
    implicit val aw = Json.writes[Area]

    Future.successful(Ok(Json.toJson(nodes.map(_.area))))
  }

  def reverse(lat: Double, lon: Double) = Action.async { request =>

    def areasContaining(pt: Point, area: GraphNode, stack: Seq[GraphNode]): Seq[Seq[GraphNode]] = {
      Logger.info("Checking area: " + renderAreaStack(stack) + " / " + area.area.name.getOrElse(""))

      val matchingChildren = area.children.toSeq.filter { c =>
        val childPolygon = polygonForPoints(c.area.points)
        childPolygon.map { p =>
          OperatorContains.local().execute(p, pt, sr, null)
        }.getOrElse {
          Logger.warn("Area has no polygon: " + c.area.name)
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

  private def renderAreaStack(stack: Seq[GraphNode]) = {
    stack.map(a => a.area.name.getOrElse("?")).mkString(" / ")
  }

  private def polygonForPoints(points: Seq[graph.Point]): Option[Polygon] = {
    points.headOption.map { n =>
      val polygon = new Polygon()
      polygon.startPath(n.lat, n.lon)
      points.drop(1).map { on =>
        polygon.lineTo(on.lat, on.lon)
      }
      polygon
    }
  }

  private def parseComponents(q: String): Seq[Long] = {
    q.split("/").toSeq.filter(_.nonEmpty).map(_.toLong)
  }

  private def nodesFor(components: Seq[Long]): mutable.Seq[GraphNode] = {
    def areaIdentifier(area: Area): Long = {
      area.id
    }

    val nodes = mutable.ListBuffer[GraphNode]()
    var show = graphService.head
    nodes.+=(show)

    val queue = new mutable.Queue() ++ components

    while (queue.nonEmpty) {
      val next = queue.dequeue()
      val children = show.children

      val found = children.find { a =>
        areaIdentifier(a.area) == next
      }

      found.map { f =>
        nodes.+=(f)
        show = f
      }
    }

    nodes
  }

}