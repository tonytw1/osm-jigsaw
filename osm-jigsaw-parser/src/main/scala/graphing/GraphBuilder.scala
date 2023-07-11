package graphing

import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.{Operator, OperatorContains, OperatorConvexHull}
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import progress.ProgressCounter
import resolving.{BoundingBox, PolygonBuilding}

import java.util
import scala.collection.mutable

class GraphBuilder extends BoundingBox with PolygonBuilding with Logging with AreaComparison {

  private val eol = GraphNode(area = Area(-1L, null, (0, 0, 0, 0), area = 0, convexHull = None))  // A null would have been better

  def buildGraph(headArea: Area, areas: Seq[Area]): GraphNode = {
    val totalAreas = areas.size // .size seems to be vaguely O^n so catch it
    logger.info("Building graph from " + totalAreas + " areas using thread " + Thread.currentThread().getId)
    logger.info("Sorting areas")
    val areas1 = areas.sortBy(-_.area)
    val head = GraphNode(headArea)
    logger.info("Map")
    val nodes = areas1.map(GraphNode(_))
    logger.info("Insert")
    head.insert(nodes)
    logger.info("Sift down")

    val queue: util.ArrayDeque[GraphNode] = new util.ArrayDeque[GraphNode]()
    queue.add(eol)
    queue.add(head)
    head.sifted = true

    var depth = 0;
    var done = 0
    while (!queue.isEmpty) {
      val node = queue.poll()
      if (node == eol && !queue.isEmpty) {
        depth = depth + 1;
        queue.add(eol)
      }
      siftDown(node, queue, depth)
      done += 1
      logger.info(done + " / " + totalAreas + " areas sifted down")
    }

    head
  }

  def siftDown(a: GraphNode, queue: util.ArrayDeque[GraphNode], depth: Int): Unit = {
    val toSift = a.children
    if (toSift.nonEmpty) {
      val taskName = if (a.area.osmIds.nonEmpty) {
        a.area.osmIds.mkString(" ,")
      } else {
        a.area.id.toString
      }
      val topLevel = siftDown(taskName, toSift, queue, depth)
      a.children = topLevel
    }
  }

  private def siftDown(taskName: String, toSift: mutable.Set[GraphNode], queue: util.ArrayDeque[GraphNode], depth: Int) = {
    logger.info("Sifting down: " + taskName + " with " + toSift.size + " children at depth " + depth)

    val topLevelNodes = mutable.Set[GraphNode]()

    //logger.debug("Presorting by area to assist sift down effectiveness")
    val inOrder = toSift.toSeq.sortBy(-_.area.area)

    val progressMessage: (Long, Option[Long], Long, Double) => String = (i: Long, total: Option[Long], delta: Long, rate: Double) => {
      "Sifted down " + i + " / " + total.get + " for " + taskName + " in " + delta + "ms at " + rate + " per second." +
        " " + topLevelNodes.size + " areas at top level"
    }

    val counter = new ProgressCounter(1000, Some(inOrder.size), Some(taskName))
    inOrder.foreach { b =>
      counter.withProgress(siftDownNode(topLevelNodes, b), progressMessage)
    }

    // Will never appear in another sift down so can be deaccelerated
    topLevelNodes.foreach { c =>
      Operator.deaccelerateGeometry(c.area.polygon)
      c.area.convexHull.foreach { ch =>
        Operator.deaccelerateGeometry(ch)
      }
      c.area.convexHull = None
    }

    topLevelNodes.foreach { c =>
      if (!c.sifted) {
        queue.offer(c)
        c.sifted = true
      }
    }
    topLevelNodes
  }

  def siftDownNode(existingSiblings: mutable.Set[GraphNode], b: GraphNode): Unit = {
    // Compare b with the existing toplevel siblings.
    // If b fits in any of them, add it as a child of those siblings.
    // else add it to the top level siblings

    val existingSiblingsWhichNewValueWouldFitIn = existingSiblings.par.filter { s =>
      areaContains(s.area, b.area)
    }

    if (existingSiblingsWhichNewValueWouldFitIn.nonEmpty) {
      existingSiblingsWhichNewValueWouldFitIn.foreach { s =>
        s.children.add(b)
      }

    } else {
      OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      if (b.area.convexHull.isEmpty) {
        val convexHull = OperatorConvexHull.local().execute(b.area.polygon, null)
        OperatorContains.local().accelerateGeometry(convexHull, sr, GeometryAccelerationDegree.enumMedium)
        b.area.convexHull = Some(convexHull)
      }
      existingSiblings.add(b)
    }
    Unit
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.osmIds).mkString(", ")
  }

}
