package graph

import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import outputgraphnodev2.OutputGraphNodeV2
import progress.ProgressCounter
import resolving.{BoundingBox, PolygonBuilding}

import java.io.InputStream
import scala.collection.mutable

class GraphReader extends Logging with PolygonBuilding with BoundingBox {

  def loadGraph(graphInput: InputStream, areas: Seq[Area]): Option[GraphNode] = {
    val tilePolygon = makePolygonD((-180, 90), (180, -90))

    val planet = Area(id = -1L, polygon = tilePolygon, boundingBox = boundingBoxFor(tilePolygon), osmIds = mutable.ListBuffer[String](), area = 0.0) // TODO If we need this root node then it should be in the graph and areas file
    val areasMap = areas.map(a => (a.id, a)).toMap + (planet.id -> planet)

    val nodes = mutable.Map[Long, GraphNode]()

    def toGraphNode(ogn: OutputGraphNodeV2): GraphNode = {
      val area = areasMap(ogn.area)
      // Map the children; leaf nodes appear first in the input file so will always have been created before been referenced
      val children = mutable.Set(ogn.children.map { childId => // TODO really needs to be mutable or not?
        nodes(childId)
      }.toSet).flatten
      GraphNode(area = area, children = children)
    }

    try {
      val counterSecond = new ProgressCounter(step = 100, label = Some("Reading graph"))
      var ok = true

      var root: GraphNode = null
      while (ok) {
        counterSecond.withProgress {
          ok = OutputGraphNodeV2.parseDelimitedFrom(graphInput).map { oa =>
            val node = toGraphNode(oa)
            root = node
            nodes.put(node.area.id, node)
            node
          }.nonEmpty
        }
      }
      graphInput.close()

      logger.info("Finished reading")
      logger.info("Head node is: " + root.area.id)
      Some(root)

    } catch {
      case e: Exception =>
        logger.error("Error: " + e)
        throw e
    }
  }

}
