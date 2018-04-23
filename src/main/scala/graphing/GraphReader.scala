package graphing

import java.io.OutputStream

import com.esri.core.geometry.{OperatorContains, Point, SpatialReference}
import model.GraphNode
import outputarea.OutputArea
import progress.ProgressCounter

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

  def all(node: GraphNode, output: mutable.Buffer[Seq[GraphNode]], parents: Seq[GraphNode] = Seq()): Unit = {
    val path = parents :+ node
    if (node.children.isEmpty) {
      output.+=(path)
    } else {
      node.children.map { c =>
        all(c, output, path)
      }
    }
  }

  def dump(node: GraphNode, soFar: String = ""): Unit = {
    val path = soFar + " / " + node.area.name + node.area.osmId.map(o => " (" + o + ")").getOrElse("")
    if (node.children.nonEmpty) {
      node.children.map( c => dump(c, path))
    } else {
      println(path)
    }
  }

  def export(node: GraphNode, output: OutputStream, parent: Option[String], count: ProgressCounter): Unit = {
    val shape = OutputArea(node.area.osmId, Some(node.area.name), parent)
    count.withProgress {
      shape.writeDelimitedTo(output)
    }
    node.children.map( c => export(c, output, node.area.osmId, count))
  }

}
