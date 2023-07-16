package graph

import model.{GraphNode, OsmIdParsing}
import outputgraphnodev2.OutputGraphNodeV2
import play.api.Logger
import progress.ProgressCounter

import java.io.{BufferedInputStream, FileNotFoundException}
import java.net.URL
import javax.inject.Inject
import scala.collection.mutable

class GraphReader @Inject()(areasReader: AreasReader) extends OsmIdParsing {

  def loadGraph(graphFile: URL): Option[GraphNode] = {
    try {

      val nodes = mutable.Map[Long, GraphNode]()

      def toGraphNode(ogn: OutputGraphNodeV2): GraphNode = {
        val area = areasReader.getAreas()(ogn.area)
        // Map the children; leaf nodes appear first in the input file so will always have been created before been referenced
        val children = ogn.children.map { childId =>
          nodes(childId)
        }
        GraphNode(area = area, children = children)
      }

      try {
        val input = new BufferedInputStream(graphFile.openStream())

        val counterSecond = new ProgressCounter(step = 100, label = Some("Reading graph"))
        var ok = true

        var root: GraphNode = null
        while (ok) {
          counterSecond.withProgress {
            ok = OutputGraphNodeV2.parseDelimitedFrom(input).map { oa =>
              val node = toGraphNode(oa)
              root = node
              nodes.put(node.area.id, node)
              node
            }.nonEmpty
          }
        }
        input.close()

        Logger.info("Finished reading")
        Logger.info("Head node is: " + root.area.id)
        Some(root)

      } catch {
        case _: FileNotFoundException =>
          Logger.warn("No segment found")
          None
        case e: Exception =>
          throw e
      }

    } catch {
      case e: Exception =>
        Logger.error("Error: " + e)
        throw e
    }
  }

}
