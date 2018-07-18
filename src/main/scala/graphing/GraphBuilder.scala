package graphing

import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.{Operator, OperatorContains}
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import org.joda.time.{DateTime, Duration}
import progress.ProgressCounter
import resolving.{BoundingBox, PolygonBuilding}

class GraphBuilder extends BoundingBox with PolygonBuilding with Logging with AreaComparison {

  def buildGraph(headArea: Area, areas: Seq[Area]): GraphNode = {
    logger.info("Building graph from " + areas.size + " areas")
    logger.info("Starting area sort")
    var head = GraphNode(headArea)
    head.insert(areas)
    siftDown(head)
    head
  }

  def siftDown(a: GraphNode): Unit = {
    logger.debug("Sifting down: " + a.area.osmIds  + " with " + a.children.size + " children")
    logger.debug("Presorting by area to assist sift down effectiveness")
    val sorted = a.children.toSeq.sortBy(_.area.area)
    val inOrder = sorted.reverse

    //OperatorContains.local().accelerateGeometry(a.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
    a.children = Set()

    val counter = new ProgressCounter(1000, Some(inOrder.size), Some(a.area.osmIds.mkString(",")))
    inOrder.foreach { b =>
      //OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      counter.withProgress {
        siftDown(a, b)
      }
    }

    a.children.par.foreach(c => Operator.deaccelerateGeometry(c.area.polygon))

    a.children.par.filter(i => i.children.nonEmpty && i.children.size > 1).foreach { c =>
      logger.debug("Sifting down from " + a.area.osmIds + " to " + c.area.osmIds)
      siftDown(c)
    }
  }

  def siftDown(a: GraphNode, b: GraphNode): Unit = {
    var start = DateTime.now()
    var siblings = a.children.filter(c => c != b)

    var startFilter = DateTime.now()
    val existingSiblingsWhichNewValueWouldFitIn = siblings.par.filter { s =>
      areaContains(s.area, b.area)
    }
    val filterDuration = new Duration(startFilter, DateTime.now)
    var secondFilterDuration: Option[Duration] = None

    if (existingSiblingsWhichNewValueWouldFitIn.nonEmpty) {
      a.children = a.children - b
      existingSiblingsWhichNewValueWouldFitIn.foreach { s =>
        logger.debug("Found sibling which new value " + b.area.osmIds + " would fit in: " + s.area.osmIds)
        s.children = s.children + b
      }

    } else {
      logger.debug("Inserting " + b.area.osmIds + " into " + a.area.osmIds)
      OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      a.children = a.children ++ Seq(b)

      val startSecondFilter = DateTime.now()
      val siblingsWhichFitInsideNewNode = siblings.par.filter { s =>
        areaContains(b.area, s.area)
      }
      secondFilterDuration = Some(new Duration(startSecondFilter, DateTime.now))

      if (siblingsWhichFitInsideNewNode.nonEmpty) {
        logger.debug("Found " + siblingsWhichFitInsideNewNode.size + " siblings to sift down into new value " + b.area.osmIds)
        a.children = a.children -- siblingsWhichFitInsideNewNode
        b.children = b.children ++ siblingsWhichFitInsideNewNode
      }
    }

    val duration = new Duration(start, DateTime.now)
    logger.debug("Sift down " + siblings.size + " took " + duration.getMillis + " filter " + filterDuration.getMillis + ", second filter: " + secondFilterDuration.map(d => d.getMillis))
    Unit
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.osmIds).mkString(", ")
  }

}
