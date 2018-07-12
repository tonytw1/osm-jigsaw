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

      def outputAreaToArea(oa: OutputArea): Area = {
        val points = (oa.latitudes zip oa.longitudes).map(ll => Point(ll._1, ll._2)).toArray
        Area(id = oa.id.get, points = points, osmIds = oa.osmIds.map(toOsmId)) // TODO Naked get of id
      }

      val areasMap = mutable.Map[Long, Area]()
      val input = new BufferedInputStream(areasFile.openStream())
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
      input.close()
      areasMap.toMap
    }

    try {
      val areas = loadAreas(areasFile)

      def getCachedArea(id: Long): Area = {
        areas.get(id).get
      }

      def toGraphNode(oa: OutputGraphNode) = {
        GraphNode(area = getCachedArea(oa.area.get))
      }

      val input = new BufferedInputStream(graphFile.openStream())
      val graphNode = OutputGraphNode.parseDelimitedFrom(input).get
      Logger.info("Graph node: " + graphNode)
      val head = toGraphNode(graphNode)
      Logger.info("Head element: " + head)

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

            val node = toGraphNode(oa)
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