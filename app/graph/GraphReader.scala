package graph

import java.io.InputStream

import com.esri.core.geometry.{Point, Polygon}
import outputarea.OutputArea
import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable

class GraphReader {

  def loadGraph(input: InputStream): Area = {
    val head = Area(None, None)
    val stack = mutable.Stack[Area]()
    stack.push(head)

    val counter = new ProgressCounter(step = 10000, label = Some("Reading graph"))
    var ok = true
    while (ok) {
      val outputArea = OutputArea.parseDelimitedFrom(input)
      outputArea.map { a =>
        counter.withProgress {
          val points = (a.latitudes zip a.longitudes).map(ll => (ll._1, ll._2))
          val polygon = polygonForPoints(points)

          val area = Area(id = a.id, name = a.name, polygon = polygon)

          var insertInto = stack.pop
          while (insertInto.id != a.parent) {
            insertInto = stack.pop
          }

          insertInto.children += area
          stack.push(insertInto)
          stack.push(area)
        }
      }
      ok = outputArea.nonEmpty

    }
    Logger.info("Finished reading")

    input.close()
    head
  }

  def polygonForPoints(points: Seq[(Double, Double)]): Option[Polygon] = {
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

case class Area(id: Option[String] = None, name: Option[String] = None, children: mutable.Set[Area] = mutable.Set(), polygon: Option[Polygon] = None)
