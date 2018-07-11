package graph

import java.io.BufferedInputStream
import java.net.URL

import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable
import outputgraphnode.OutputGraphNode

class GraphReader {

  def loadGraph(graphFile: URL): GraphNode = {

    def toGraphNode(oa: OutputGraphNode) = {
      val points = (oa.latitudes zip oa.longitudes).map(ll => Point(ll._1, ll._2)).toArray
      val osmIds = oa.osmIds.toArray
      GraphNode(id = oa.id.get, osmIds = osmIds, points = points)
    }

    try {
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
            while (Some(insertInto.id) != oa.parent) {
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

      input.close()
      head

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

}

case class Point(lat: Double, lon: Double)

case class GraphNode(id: Long, children: mutable.ListBuffer[GraphNode] = mutable.ListBuffer(), osmIds: Seq[String] = Seq(), points: Seq[Point] = Seq())