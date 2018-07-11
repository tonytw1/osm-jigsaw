package graphing

import java.io.OutputStream

import com.esri.core.geometry.SpatialReference
import model.GraphNode
import outputgraphnode.OutputGraphNode
import progress.ProgressCounter

class GraphReader {

  val sr = SpatialReference.create(1)

  def export(node: GraphNode, output: OutputStream, parent: Option[Long], count: ProgressCounter): Unit = {
    val latitudes = node.area.polygon.getCoordinates2D.map(p => p.x)
    val longitudes = node.area.polygon.getCoordinates2D.map(p => p.y)

    val outputGraphNode = OutputGraphNode(id = Some(node.area.id), parent = parent, osmIds = node.area.osmIds,
      latitudes = latitudes, longitudes = longitudes
    )
    count.withProgress {
      outputGraphNode.writeDelimitedTo(output)
    }
    node.children.map( c => export(c, output, Some(node.area.id), count))
  }

}
