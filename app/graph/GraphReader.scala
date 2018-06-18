package graph

import java.io.InputStream

import com.esri.core.geometry.Polygon
import outputarea.OutputArea
import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable

class GraphReader {

  def loadGraph(input: InputStream): Area = {
    val head = outputAreaToArea(OutputArea.parseDelimitedFrom(input).get)
    Logger.info("Head element: " + head)
    val stack = mutable.Stack[Area]()
    stack.push(head)

    val counter = new ProgressCounter(step = 10000, label = Some("Reading graph"))
    var ok = true
    while (ok) {
      val outputArea: Option[OutputArea] = OutputArea.parseDelimitedFrom(input)
      outputArea.map { oa =>
        counter.withProgress {
          val area = outputAreaToArea(oa)
          // Logger.info("Processing area: " + area.name + " / " + area.id + " / " + area.parent)
          var insertInto = stack.pop
          while (Some(insertInto.id) != oa.parent) {
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

  private def outputAreaToArea(oa: OutputArea): Area = {
    val points = (oa.latitudes zip oa.longitudes).map(ll => (ll._1, ll._2))
    val polygon = polygonForPoints(points)
    Area(id = oa.id.get, name = oa.name, polygon = polygon, parent = oa.parent)   // TODO Naked get of id
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

case class Area(id: Long, name: Option[String] = None, children: mutable.Set[Area] = mutable.Set(), polygon: Option[Polygon] = None, parent: Option[Long])
