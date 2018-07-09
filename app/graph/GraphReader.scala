package graph

import java.io.BufferedInputStream
import java.net.URL

import outputarea.OutputArea
import outputgraphnode.OutputGraphNode
import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable

class GraphReader {

  def loadGraph(areasFile: URL, graphFile: URL): GraphNode = {
    try {
      val input = new BufferedInputStream(areasFile.openStream())

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

      val inputSecond = new BufferedInputStream(graphFile.openStream())
      val graphNode = OutputGraphNode.parseDelimitedFrom(inputSecond).get
      Logger.info("Graph node: " + graphNode)
      val head = GraphNode(area = getCachedArea(graphNode.area.get))
      Logger.info("Head element: " + head)

      val stack = mutable.Stack[GraphNode]()
      stack.push(head)

      val counterSecond = new ProgressCounter(step = 10000, label = Some("Building graph"))
      ok = true
      while (ok) {
        val outputGraphNode = OutputGraphNode.parseDelimitedFrom(inputSecond)
        outputGraphNode.map { oa =>
          counterSecond.withProgress {
            var insertInto = stack.pop
            while (Some(insertInto.area.id) != oa.parent) {
              insertInto = stack.pop
            }

            val node = GraphNode(area = getCachedArea(oa.area.get))

            insertInto.children += node

            stack.push(insertInto)
            stack.push(node)
          }
        }
        ok = outputGraphNode.nonEmpty

      }
      inputSecond.close()

      Logger.info("Finished reading")
      input.close()

      def dedupeChildren(graphNode: GraphNode): GraphNode = {

        if (graphNode.children.nonEmpty) {
          val deduped: mutable.ListBuffer[GraphNode] = graphNode.children.map { c =>
            graphNode.children.find { cl =>
              c.childAreas == cl.childAreas
            }.getOrElse(c)
          }

          if (deduped.toSet.size != graphNode.children.toSet.size) {
            Logger.info("Deduped: " + graphNode.area.osmId + " was " + graphNode.children.toSet.size + " now " + deduped.toSet.size)
          }

          graphNode.children = deduped

          graphNode.children.map { c =>
            dedupeChildren(c)
          }

          graphNode

        } else {
          graphNode
        }

      }

      Logger.info("Packing graph to remove duplications")

      dedupeChildren(head)

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

  private def outputAreaToArea(oa: OutputArea): Area = {
    val points = (oa.latitudes zip oa.longitudes).map(ll => Point(ll._1, ll._2)).toArray
    // TODO Naked get of id
    Area(id = oa.id.get, points = points, osmId = oa.osmId)
  }

}

case class Point(lat: Double, lon: Double)

case class Area(id: Long, points: Seq[Point], osmId: Option[String]) {
  override def hashCode() = id.hashCode()
}

case class GraphNode(area: Area, var children: mutable.ListBuffer[GraphNode] = mutable.ListBuffer()) {

  def childAreas: Set[Area] = {
    children.map(gh => gh.area).toSet
  }

}