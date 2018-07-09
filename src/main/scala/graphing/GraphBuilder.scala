package graphing

import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.{Operator, OperatorContains}
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import org.joda.time.{DateTime, Duration}
import progress.ProgressCounter
import resolving.{BoundingBox, PolygonBuilding}

import scala.collection.mutable

class GraphBuilder extends BoundingBox with PolygonBuilding with Logging with AreaComparison {

  def buildGraph(headArea: Area, areas: Seq[Area]): GraphNode = {
    logger.info("Building graph from " + areas.size + " areas")

    logger.info("Deduplicating areas")
    logger.info("Sorting areas by size")
    val sortedAreas = areas.sortBy(_.area)


    val deduplicationCounter = new ProgressCounter(1000, Some(areas.size))

    val deduplicatedAreas = mutable.ListBuffer[Area]()
    sortedAreas.foreach{ a =>
      deduplicationCounter.withProgress {
        var ok = deduplicatedAreas.nonEmpty
        val i = deduplicatedAreas.iterator
        var count = 0;
        var found: Option[Area] = None
        while(ok) {
          var x = i.next()
          ok = x.area >= a.area
          if (x.area == a.area && areaSame(x, a)) {
            found = Some(x)
          }
          count = count + 1
        }

        //logger.info("F: " + found + " after " + count)

        found.map { e =>
          logger.debug("Ignoring area for " + a.osmId + "/" + areaOf(a) + "  which is the same area as: " + e.osmId + "/" + areaOf(e)) // TODO Don't silently drop; record this fact
        }.getOrElse {
          deduplicatedAreas.+=:(a)
        }
      }
    }
    logger.info("Areas remaining after deduplication: " + deduplicatedAreas.size + "/" + areas.size)
    logger.info("Starting area sort")
    var head = GraphNode(headArea)
    head.insert(deduplicatedAreas)
    siftDown(head)
    head
  }

  def siftDown(a: GraphNode): Unit = {
    logger.debug("Sifting down: " + a.area.osmId  + " with " + a.children.size + " children")
    logger.debug("Presorting by area to assist sift down effectiveness")
    val sorted = a.children.toSeq.sortBy { a =>
      areaOf(a.area)
    }
    val inOrder = sorted.reverse

    //OperatorContains.local().accelerateGeometry(a.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
    a.children = Set()

    val counter = new ProgressCounter(1000, Some(inOrder.size), a.area.osmId)
    inOrder.foreach { b =>
      //OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      counter.withProgress {
        siftDown(a, b)
      }
    }

    a.children.par.foreach(c => Operator.deaccelerateGeometry(c.area.polygon))

    a.children.par.filter(i => i.children.nonEmpty && i.children.size > 1).foreach { c =>
      logger.debug("Sifting down from " + a.area.osmId + " to " + c.area.osmId)
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
      existingSiblingsWhichNewValueWouldFitIn.map { s =>
        logger.debug("Found sibling which new value " + b.area.osmId + " would fit in: " + s.area.osmId)
        s.children = s.children + b
      }

    } else {
      logger.debug("Inserting " + b.area.osmId + " into " + a.area.osmId)
      OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      a.children = a.children ++ Seq(b)

      val startSecondFilter = DateTime.now()
      val siblingsWhichFitInsideNewNode = siblings.par.filter { s =>
        areaContains(b.area, s.area)
      }
      secondFilterDuration = Some(new Duration(startSecondFilter, DateTime.now))

      if (siblingsWhichFitInsideNewNode.nonEmpty) {
        logger.debug("Found " + siblingsWhichFitInsideNewNode.size + " siblings to sift down into new value " + b.area.osmId)
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
    nodes.map(s => s.area.osmId).mkString(", ")
  }

}
