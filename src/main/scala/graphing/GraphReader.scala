package graphing

import java.io.OutputStream

import com.esri.core.geometry.SpatialReference
import model.GraphNode
import outputgraphnode.OutputGraphNode
import progress.ProgressCounter

class GraphReader {

  val sr = SpatialReference.create(1)

  def export(node: GraphNode, output: OutputStream, parent: Option[Long], count: ProgressCounter): Unit = {
    count.withProgress {
      OutputGraphNode(area = Some(node.area.id), parent = parent).writeDelimitedTo(output)
    }
    node.children.map( c => export(c, output, Some(node.area.id), count))
  }

}
