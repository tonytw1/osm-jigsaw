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

    var head = GraphNode(headArea)

    head.insert(areas)
    siftDown(head)
    head
  }

  def siftDown(a: GraphNode): Unit = {
    logger.info("Sifting down: " + a.area.name  + " with " + a.children.size + " children")
    logger.debug("Presorting by area to assist sift down effectiveness")
    val sorted = a.children.toSeq.sortBy { a =>
      areaOf(a.area)
    }
    val inOrder = sorted.reverse

    //OperatorContains.local().accelerateGeometry(a.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
    a.children = Set()

    val counter = new ProgressCounter(1000, Some(inOrder.size), Some(a.area.name))
    inOrder.foreach { b =>
      //OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      counter.withProgress {
        siftDown(a, b)
      }
    }

    a.children.par.foreach(c => Operator.deaccelerateGeometry(c.area.polygon))

    a.children.par.filter(i => i.children.nonEmpty && i.children.size > 1).foreach { c =>
      logger.info("Sifting down from " + a.area.name + " to " + c.area.name)
      siftDown(c)
    }
  }

  def siftDown(a: GraphNode, b: GraphNode): Unit = {
    var start = DateTime.now()
    var siblings = a.children.filter(c => c != b)

    var startFilter = DateTime.now()
    val existingSiblingsWhichNewValueWouldFitIn = siblings.filter { s =>
      areaContains(s.area, b.area)
    }
    val filterDuration = new Duration(startFilter, DateTime.now)
    var secondFilterDuration: Option[Duration] = None

    if (existingSiblingsWhichNewValueWouldFitIn.nonEmpty) {
      a.children = a.children - b
      existingSiblingsWhichNewValueWouldFitIn.map { s =>
        logger.debug("Found sibling which new value " + b.area.name + " would fit in: " + s.area.name)
        s.children = s.children + b
      }

    } else {
      logger.debug("Inserting " + b.area.name + " into " + a.area.name)
      OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      a.children = a.children ++ Seq(b)

      val startSecondFilter = DateTime.now()
      val siblingsWhichFitInsideNewNode = siblings.filter { s =>
        areaContains(b.area, s.area)
      }
      secondFilterDuration = Some(new Duration(startSecondFilter, DateTime.now))

      if (siblingsWhichFitInsideNewNode.nonEmpty) {
        logger.debug("Found " + siblingsWhichFitInsideNewNode.size + " siblings to sift down into new value " + b.area.name)
        a.children = a.children -- siblingsWhichFitInsideNewNode
        b.children = b.children ++ siblingsWhichFitInsideNewNode
      }

      //val siblingsWhichOverlapWithNewNode = siblings.filter(c => c != b).filter{ s =>
      //  areasOverlap(b.area, s.area)
      //}

      /*
      siblingsWhichOverlapWithNewNode.map { s =>
        logger.info("New area " + b.area.name + " intersects with sibling: " + s.area.name + " which has " + s.children.size + " children")

        val inOverlap = s.children.filter{ sc =>
          areaContains(b.area, sc.area)
        }

        logger.info("Found " + inOverlap.size + " overlap children to copy to new area")
        b.children = b.children ++ inOverlap
        inOverlap.map { u =>
          siftDown(b, u)
        }
      }
      */
    }

    val duration = new Duration(start, DateTime.now)
    logger.debug("Sift down " + siblings.size + " took " + duration.getMillis + " filter " + filterDuration.getMillis + ", second filter: " + secondFilterDuration.map(d => d.getMillis))
    Unit
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.name).mkString(", ")
  }

}
