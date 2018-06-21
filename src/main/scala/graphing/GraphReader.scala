package graphing

import java.io.OutputStream

import com.esri.core.geometry.{OperatorContains, Point, SpatialReference}
import model.{Area, GraphNode}
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

  def export(node: GraphNode, output: OutputStream, parent: Option[Long], count: ProgressCounter): Unit = {
    val latitudes = mutable.ListBuffer[Double]()
    val longitudes = mutable.ListBuffer[Double]()
    val pointCount = node.area.polygon.getPointCount - 1
    (0 to pointCount).map { i =>
      val p = node.area.polygon.getPoint(i)
      latitudes.+=(p.getX)
      longitudes.+=(p.getY)
    }

    val outputArea = OutputArea(id = Some(node.area.id), osmId = node.area.osmId, name = Some(node.area.name), parent = parent, latitudes = latitudes, longitudes = longitudes)
    count.withProgress {
      outputArea.writeDelimitedTo(output)
    }
    node.children.map( c => export(c, output, Some(node.area.id), count))
  }

  def exportAreas(areas: Seq[Area], output: OutputStream, count: ProgressCounter): Unit = {
    areas.map { area =>
      val latitudes = mutable.ListBuffer[Double]()
      val longitudes = mutable.ListBuffer[Double]()
      val pointCount = area.polygon.getPointCount - 1
      (0 to pointCount).map { i =>
        val p = area.polygon.getPoint(i)
        latitudes.+=(p.getX)
        longitudes.+=(p.getY)
      }

      val outputArea = OutputArea(id = Some(area.id), osmId = area.osmId, name = Some(area.name), parent = None, latitudes = latitudes, longitudes = longitudes)
      count.withProgress {
        outputArea.writeDelimitedTo(output)
      }
    }
  }

}
