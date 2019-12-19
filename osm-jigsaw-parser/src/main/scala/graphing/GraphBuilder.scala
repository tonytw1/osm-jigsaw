package graphing

import areas.AreaComparison
import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.{Operator, OperatorContains, Polygon}
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import progress.ProgressCounter
import resolving.{BoundingBox, PolygonBuilding}

import scala.collection.mutable.ListBuffer

class GraphBuilder extends BoundingBox with PolygonBuilding with Logging with AreaComparison {

  def buildGraph(headArea: Area, areas: Seq[Area]): GraphNode = {
    logger.info("Building graph from " + areas.size + " areas")
    logger.debug("Starting area sort")
    var head = GraphNode(headArea)
    head.insert(areas.sortBy(-_.area))
    siftDown(head)
    head
  }

  def siftDown(a: GraphNode): Unit = {
    //logger.debug("Sifting down: " + a.area.osmIds  + " with " + a.children.size + " children")
    //logger.debug("Presorting by area to assist sift down effectiveness")
    val inOrder = a.children // .sortBy(-_.area.area)

    val areas = a.children.map { a => a.area.area}

    //OperatorContains.local().accelerateGeometry(a.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
    a.children = ListBuffer()

    val counter = new ProgressCounter(10000, Some(inOrder.size), Some(a.area.osmIds.mkString(",")))
    inOrder.foreach { b =>
      //logger.info("B: " + a.area.id + " " + b.area.area)
      //OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      counter.withProgress {
        siftDown(a, b)
      }
    }

    a.children.par.foreach(c => {
      Operator.deaccelerateGeometry(c.area.polygon)
    })

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
      s != b && areaContains(s.area, b.area)
    }
    //val filterDuration = new Duration(startFilter, DateTime.now)
    //var secondFilterDuration: Option[Duration] = None

    if (existingSiblingsWhichNewValueWouldFitIn.nonEmpty) {
      existingSiblingsWhichNewValueWouldFitIn.foreach { s =>
        // logger.debug("Found sibling which new value " + b.area.osmIds + " would fit in: " + s.area.osmIds)
        s.children = s.children :+ b.copy()
      }

    } else {
      // logger.debug("Inserting " + b.area.osmIds + " into " + a.area.osmIds)
      val geometry = b.area.polygon
      OperatorContains.local().accelerateGeometry(geometry, sr, GeometryAccelerationDegree.enumMedium)
      a.children = a.children :+ b
    }

    // val duration = new Duration(start, DateTime.now)
    // logger.debug("Sift down " + siblings.size + " took " + duration.getMillis + " filter " + filterDuration.getMillis + ", second filter: " + secondFilterDuration.map(d => d.getMillis))
    Unit
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.osmIds).mkString(", ")
  }

}
