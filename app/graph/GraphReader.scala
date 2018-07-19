package graph

import java.io.BufferedInputStream
import java.net.URL

import model.OsmIdParsing
import outputarea.OutputArea
import outputgraphnode.OutputGraphNode
import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable

class GraphReader extends OsmIdParsing {

  def loadGraph(areasFile: URL, graphFile: URL): GraphNode = {

    def loadAreas(areasFile: URL): Map[Long, Area] = {

      def outputAreaToArea(oa: OutputArea): Option[Area] = {
        oa.id.map { id =>
          val points = (oa.latitudes zip oa.longitudes).map(ll => Point(ll._1, ll._2)).toArray
          Area(id = oa.id.get, points = points, osmIds = oa.osmIds.map(toOsmId))
        }
      }

      val areasMap = mutable.Map[Long, Area]()
      val input = new BufferedInputStream(areasFile.openStream())
      val counter = new ProgressCounter(step = 100000, label = Some("Reading areas"))
      var ok = true
      while (ok) {
        counter.withProgress {
          val added = OutputArea.parseDelimitedFrom(input).flatMap { oa =>
            outputAreaToArea(oa).map { area =>
              areasMap += area.id -> area
              area
            }
          }
          ok = added.nonEmpty
        }
      }
      Logger.info("Mapped areas: " + areasMap.size)
      input.close()
      areasMap.toMap
    }

    try {
      val areas = loadAreas(areasFile)



      def toGraphNode(ogn: OutputGraphNode): Option[GraphNode] = {
        ogn.area.flatMap { areaId =>
          areas.get(areaId).map { area =>
            GraphNode(area = area)
          }
        }
      }

      val input = new BufferedInputStream(graphFile.openStream())
      val head = toGraphNode(OutputGraphNode.parseDelimitedFrom(input).get).get

      val stack = mutable.Stack[GraphNode]()
      stack.push(head)

      val counterSecond = new ProgressCounter(step = 10000, label = Some("Building graph"))
      var ok = true
      while (ok) {
        val outputGraphNode = OutputGraphNode.parseDelimitedFrom(input)
        outputGraphNode.map { oa =>
          counterSecond.withProgress {
            var insertInto = stack.pop
            while (Some(insertInto.area.id) != oa.parent) {
              insertInto = stack.pop
            }

            val node = toGraphNode(oa).get
            insertInto.children += node

            stack.push(insertInto)
            stack.push(node)
          }
        }
        ok = outputGraphNode.nonEmpty

      }
      input.close()

      Logger.info("Finished reading")
      head

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

}

case class Area(id: Long, points: Seq[Point], osmIds: Seq[OsmId]) {
  override def hashCode() = id.hashCode()
}

case class Point(lat: Double, lon: Double)
case class OsmId(id: Long, `type`: Char)
case class GraphNode(area: Area, children: mutable.ListBuffer[GraphNode] = mutable.ListBuffer())