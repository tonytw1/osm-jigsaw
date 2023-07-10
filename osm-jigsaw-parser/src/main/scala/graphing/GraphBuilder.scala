package graphing

import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.{Operator, OperatorContains, Polygon, OperatorConvexHull}
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import progress.ProgressCounter
import resolving.{BoundingBox, PolygonBuilding}

import java.util
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class GraphBuilder extends BoundingBox with PolygonBuilding with Logging with AreaComparison {

  private val sifts = mutable.Map[Area, Long]()

  def buildGraph(headArea: Area, areas: Seq[Area]): GraphNode = {
    logger.info("Building graph from " + areas.size + " areas using thread " + Thread.currentThread().getId)
    logger.info("Sorting areas")
    val areas1 = areas.sortBy(-_.area)
    val head = GraphNode(headArea)
    logger.info("Map")
    val nodes = areas1.map(GraphNode(_))
    logger.info("Insert")
    head.insert(nodes)
    logger.info("Sift down")


    val queue: util.ArrayDeque[GraphNode] = new util.ArrayDeque[GraphNode]()
    queue.add(head)
    head.sifted = true

    var done = 0
    while (!queue.isEmpty) {
      val node = queue.poll()
      logger.info(done + " / " + areas.size + " areas sifted down")
      done += 1
      siftDown(node, queue)
    }

    head
  }

  def siftDown(a: GraphNode, queue: util.ArrayDeque[GraphNode]): Unit = {
    if (a.children.nonEmpty) {

      logger.info("Sifting down: " + a.area.osmIds.mkString(",") + " with " + a.children.size + " children")
      //logger.debug("Presorting by area to assist sift down effectiveness")
      val inOrder = a.children.toSeq.sortBy(-_.area.area)

      a.children = mutable.Set()

      val counter = new ProgressCounter(1000, Some(inOrder.size), Some(a.area.osmIds.mkString(",")))
      inOrder.foreach { b =>
        val progressMessage: (Long, Option[Long], Long, Double) => String = (i: Long, total: Option[Long], delta: Long, rate: Double) => {
          val areaName = if (a.area.osmIds.nonEmpty) {
            a.area.osmIds.mkString(" ,")
          } else {
            a.area.id.toString
          }
          "Sifted down " + i + " / " + total.get + " for " + areaName + " in " + delta + "ms at " + rate + " per second." +
            " " + a.children.size + " areas at top level"
        }
        counter.withProgress(siftDown(a, b), progressMessage)
      }

      // Will never appear in another sift down so can be deaccelerated
      a.children.foreach { c=>
        Operator.deaccelerateGeometry(c.area.polygon)
        c.area.convexHull.foreach { ch =>
          Operator.deaccelerateGeometry(ch)
        }
        c.area.convexHull = None
      }

      val ss = sifts.getOrElse(a.area, 0L)
      sifts.put(a.area, ss + 1)
      a.children.foreach { c =>
        if (!c.sifted) {
          queue.add(c)
          c.sifted = true
        }
      }
    }
  }

  def siftDown(a: GraphNode, b: GraphNode): Unit = {
    val existingSiblingsWhichNewValueWouldFitIn = a.children.par.filter { s =>
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
      a.children.add(b)
    }
    Unit
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.osmIds).mkString(", ")
  }

}
