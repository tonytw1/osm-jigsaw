package graphing

import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry._
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import progress.ProgressCounter
import resolving.PolygonBuilding

class GraphBuilder extends PolygonBuilding with Logging with AreaComparison {

  def buildGraph(headArea: Area, areas: Seq[Area]): GraphNode = {
    logger.info("Building graph from " + areas.size + " areas")
    logger.info("Starting area sort")
    var head = GraphNode(headArea)

    val generalised = areas.map { a =>
      val pathSize = a.polygon.getPathSize(0)


      if (pathSize > 10000) {
        val generalised = OperatorGeneralize.local().execute(a.polygon, .00001, false, null)

        val g = generalised.asInstanceOf[Polygon]
        val generalisedCount = g.getPathSize(0)

        logger.info(pathSize + " -> " + generalisedCount)
        a.copy(polygon = g)
      } else {
        a
      }
    }

    head.insert(generalised)
    siftDown(head)
    head
  }

  def siftDown(a: GraphNode): Unit = {
    //logger.debug("Sifting down: " + a.area.osmIds  + " with " + a.children.size + " children")
    //logger.debug("Presorting by area to assist sift down effectiveness")
    val inOrder = a.children.toSeq.sortBy(-_.area.area)

    //OperatorContains.local().accelerateGeometry(a.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
    a.children = Set()



    val counter = new ProgressCounter(1000, Some(inOrder.size), Some(a.area.osmIds.mkString(",")))
    inOrder.foreach { b =>
      //OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      counter.withProgress {
        siftDown(a, b)
      }
    }

    a.children.foreach { c =>
      Operator.deaccelerateGeometry(c.area.polygon)
      c.area.hull.map { h =>
        Operator.deaccelerateGeometry(h)
      }
    }

    a.children.filter(i => i.children.nonEmpty).par.foreach { c =>
      // logger.debug("Sifting down from " + a.area.osmIds + " to " + c.area.osmIds)
      siftDown(c)
    }
  }

  def siftDown(a: GraphNode, b: GraphNode): Unit = {
    //var start = DateTime.now()
    //var siblings = a.children// .filter(c => c != b)

    //var startFilter = DateTime.now()

    val existingSiblingsWhichNewValueWouldFitIn = a.children.filter { s =>
      areaContains(s.area, b.area)
    }
    //val filterDuration = new Duration(startFilter, DateTime.now)
    //var secondFilterDuration: Option[Duration] = None

    if (existingSiblingsWhichNewValueWouldFitIn.nonEmpty) {
      existingSiblingsWhichNewValueWouldFitIn.foreach { s =>
        // logger.debug("Found sibling which new value " + b.area.osmIds + " would fit in: " + s.area.osmIds)
        s.children = s.children + b
      }

    } else {
      logger.debug("Inserting " + b.area.osmIds + " into " + a.area.osmIds)

      val hull = OperatorConvexHull.local().execute(b.area.polygon, null)
      OperatorContains.local().accelerateGeometry(hull, sr, GeometryAccelerationDegree.enumMedium)
      OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      a.children = a.children + b.copy(area = b.area.copy(hull = Some(hull)))
    }

    // val duration = new Duration(start, DateTime.now)
    // logger.debug("Sift down " + siblings.size + " took " + duration.getMillis + " filter " + filterDuration.getMillis + ", second filter: " + secondFilterDuration.map(d => d.getMillis))
    Unit
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.osmIds).mkString(", ")
  }

}
