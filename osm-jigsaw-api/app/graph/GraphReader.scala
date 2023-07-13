package graph

import java.io.{BufferedInputStream, FileNotFoundException}
import java.net.URL

import javax.inject.Inject
import model.{GraphNode, OsmIdParsing}
import outputgraphnode.OutputGraphNode
import play.api.Logger
import progress.ProgressCounter

import scala.collection.mutable

class GraphReader @Inject()(areasReader: AreasReader) extends OsmIdParsing {

  def loadGraph(graphFile: URL): Option[GraphNode] = {
    try {
      def toGraphNode(ogn: OutputGraphNode): Option[GraphNode] = {
        val maybeNode = ogn.area.flatMap { areaId =>
          areasReader.getAreas().get(areaId).map { area =>
            GraphNode(area = area)
          }
        }
        if (maybeNode.isEmpty) {
          Logger.warn("No area found for " + ogn)
        }
        maybeNode
      }

      val stack = mutable.Stack[GraphNode]()

      try {
        val input = new BufferedInputStream(graphFile.openStream())

        val counterSecond = new ProgressCounter(step = 10000, label = Some("Building graph"))
        var ok = true
        while (ok) {
          counterSecond.withProgress {
            ok = OutputGraphNode.parseDelimitedFrom(input).flatMap { oa =>
              toGraphNode(oa).map { node =>
                val insertInto = if (stack.nonEmpty) {
                  var insertInto = stack.pop
                  while (!oa.parent.contains(insertInto.area.id)) {
                    insertInto = stack.pop
                  }
                  insertInto.children += node
                  insertInto
                } else {
                  node
                }

                stack.push(insertInto)
                stack.push(node)
                node
              }
            }.nonEmpty
          }
        }
        input.close()

        Logger.info("Finished reading")
        Logger.info("Head node is: " + stack.lastOption.map(_.area.id))
        stack.lastOption

      } catch {
        case _: FileNotFoundException => {
          Logger.warn("No segment found")
          None
        }
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
