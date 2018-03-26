package graphing

import com.esri.core.geometry.{OperatorContains, Point, SpatialReference}
import model.GraphNode

import scala.collection.mutable

class GraphReader {

  val sr = SpatialReference.create(1)

  def find(pt: Point, node: GraphNode, seenSoFar: mutable.Buffer[GraphNode]): mutable.Buffer[GraphNode] = {
    val childEnclosingPoint = node.children.find(c => OperatorContains.local().execute(c.area.polygon, pt, sr, null))
    childEnclosingPoint.map { c =>
      find(pt, c, seenSoFar.+=(node))
    }.getOrElse {
      if (OperatorContains.local().execute(node.area.polygon, pt, sr, null)) {
        seenSoFar.+=(node)
      } else {
        seenSoFar
      }
    }
  }

}
