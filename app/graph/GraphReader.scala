package graph

import java.io.{BufferedInputStream, InputStream}
import java.net.URL

import outputarea.OutputArea
import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable

class GraphReader {

  def loadGraph(file: URL): Area = {
    val input = new BufferedInputStream(file.openStream())

    val areasMap = mutable.Map[Long, Area]()
    val counter = new ProgressCounter(step = 100000, label = Some("Reading areas"))
    var ok = true
    while (ok) {
      counter.withProgress {
        val outputArea = OutputArea.parseDelimitedFrom(input)
        outputArea.map { oa =>
          val area = outputAreaToArea(oa)
          areasMap += area.id -> area
        }
        ok = outputArea.nonEmpty
      }
    }
    Logger.info("Mapped areas: " + areasMap.size)

    def getCachedArea(id: Long): Area = {
      areasMap.get(id).get
    }
    input.close()

    val inputSecond = new BufferedInputStream(file.openStream())
    val area1 = outputAreaToArea(OutputArea.parseDelimitedFrom(inputSecond).get)
    val head = getCachedArea(area1.id)
    Logger.info("Head element: " + head)
    val stack = mutable.Stack[Area]()
    stack.push(head)

    val counterSecond = new ProgressCounter(step = 1000, label = Some("Building graph"))
    ok = true
    while (ok) {
      val outputArea = OutputArea.parseDelimitedFrom(inputSecond)
      outputArea.map { oa =>
        counterSecond.withProgress {
          val area = getCachedArea(outputAreaToArea(oa).id)
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
    inputSecond.close()

    Logger.info("Finished reading")

    input.close()
    head
  }

  private def outputAreaToArea(oa: OutputArea): Area = {
    val points = (oa.latitudes zip oa.longitudes).map(ll => (ll._1, ll._2))
    Area(id = oa.id.get, name = oa.name, points = points, parent = oa.parent) // TODO Naked get of id
  }

}

case class Area(id: Long, name: Option[String] = None, children: mutable.Set[Area] = mutable.Set(), points: Seq[(Double, Double)], parent: Option[Long])
