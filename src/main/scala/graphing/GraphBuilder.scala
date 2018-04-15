package graphing

import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.OperatorContains
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import org.joda.time.{DateTime, Duration}
import progress.ProgressCounter
import resolving.{BoundingBox, PolygonBuilding}

class GraphBuilder extends BoundingBox with PolygonBuilding with Logging with AreaComparison {

  def buildGraph(areas: Seq[Area]): GraphNode = {
    logger.info("Building graph from " + areas.size + " areas")
    logger.info("Presorting by area to assist sift down effectiveness")
    var c = 0
    val sorted = areas.sortBy { a =>
      areaOf(a)
    }
    val inOrder = sorted.reverse
    logger.info("Finished sorting")

    var i = 0
    var j = 0
    val total = sorted.size

    val earthArea = makePolygon((-180, 90),(180, -90))
    val earth = Area(name = "Earth", earthArea, boundingBoxFor(earthArea))
    var head = GraphNode(earth)

    val counter = new ProgressCounter(100)
    head.insert(inOrder)
    siftDown(head)

    /*
    inOrder.foreach { a =>
      counter.withProgress {
        // OperatorContains.local().accelerateGeometry(a.polygon, sr, GeometryAccelerationDegree.enumMedium)
        siftDown(head)
      }
    }
    */

    head
  }

  def siftDown(a: GraphNode): Unit = {
    logger.info("Sifting down: " + a.area  + " with " + a.children.size + " children")
    OperatorContains.local().accelerateGeometry(a.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
    val in = a.children
    a.children = Set()

    val counter = new ProgressCounter(100)
    in.foreach { b =>
      OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      siftDown(a, b)
    }

    // TODO can undo acceleration on items which are no longer in scope
    a.children.par.map { c =>
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
        logger.info("Found sibling which new value " + b.area.name + " would fit in: " + s.area.name)
        s.children = s.children + b
        //siftDown(s, b) // TODO test case needed
      }

    } else {
      logger.info("Inserting " + b.area.name + " into " + a.area.name)
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
    logger.info("Sift down " + siblings.size + " took " + duration.getMillis + " filter " + filterDuration.getMillis + ", second filter: " + secondFilterDuration.map(d => d.getMillis))
    Unit
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.name).mkString(", ")
  }

}
