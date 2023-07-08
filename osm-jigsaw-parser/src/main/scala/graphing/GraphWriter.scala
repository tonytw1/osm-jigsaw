package graphing

import java.io.OutputStream
import model.GraphNode
import org.apache.logging.log4j.scala.Logging
import outputgraphnode.OutputGraphNode
import progress.ProgressCounter

import scala.collection.mutable

class GraphWriter extends Logging {

  def export(node: GraphNode, output: OutputStream, parent: Option[Long], count: ProgressCounter, stack: mutable.Stack[GraphNode] = mutable.Stack[GraphNode]()): Unit = {
    // Cycle detection; there should be none
    if (stack.contains(node)) {
      val ids = stack.map(_.area.osmIds.mkString(""))
      logger.warn("Loop detected: " + ids)
      throw new RuntimeException("Node already written; possible cycle involving node: " + node.area.id + " with stack: " + ids)
    }
    stack.push(node)
    count.withProgress{OutputGraphNode(area = Some(node.area.id), parent = parent).writeDelimitedTo(output)}
    node.children.foreach(c => export(c, output, Some(node.area.id), count, stack))
    stack.pop()
  }

}
