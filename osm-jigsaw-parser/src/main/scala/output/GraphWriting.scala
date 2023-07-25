package output

import model.{FlippedGraphNode, GraphNode}
import outputgraphnodev2.OutputGraphNodeV2

import java.io.OutputStream
import scala.collection.mutable

trait GraphWriting {

  def outputGraph(root: GraphNode, output: OutputStream): Unit = {
    // Now we can write out the flipped graph
    // If we DFS write leaf nodes first then all children will have already been encountered by the time they are read.
    // Given our leaf first ordering, if a node appears more than once we can skip it; the reader will have already encountered it and it's children
    val persisted = mutable.Set[Long]()

    def visit(node: GraphNode): Unit = {
      if (!persisted.contains(node.area.id)) {
        node.children.foreach { c =>
          visit(c)
        }
        persisted.add(node.area.id)
        new OutputGraphNodeV2(node.area.id, node.children.map(_.area.id).toSeq).writeDelimitedTo(output)
      }
    }

    visit(root)
    output.flush()
    output.close()
  }

  def outputFlippedGraph(root: FlippedGraphNode, output: OutputStream): Unit = {
    // Now we can write out the flipped graph
    // If we DFS write leaf nodes first then all children will have already been encountered by the time they are read.
    // Given our leaf first ordering, if a node appears more than once we can skip it; the reader will have already encountered it and it's children
    val persisted = mutable.Set[Long]()

    def visit(node: FlippedGraphNode): Unit = {
      if (!persisted.contains(node.id)) {
        node.children.foreach { c =>
          visit(c)
        }
        persisted.add(node.id)
        new OutputGraphNodeV2(node.id, node.children.map(_.id).toSeq).writeDelimitedTo(output)
      }
    }

    visit(root)
    output.flush()
    output.close()
  }

}
