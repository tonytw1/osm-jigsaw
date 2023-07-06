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
    logger.info("Building graph from " + areas.size + " areas using thread " + Thread.currentThread().getId)
    logger.info("Sorting areas")
    val areas1 = areas.sortBy(-_.area)
    val head = GraphNode(headArea)
    logger.info("Map")
    val nodes = areas1.map(GraphNode(_))
    logger.info("Insert")
    head.insert(nodes)
    logger.info("Sift down")
    siftDown(head)
    head
  }

  def siftDown(a: GraphNode): Unit = {
    if (a.children.size > 1) {
      logger.info("Sifting down: " + a.area.osmIds.mkString(",")  + " with " + a.children.size + " children")
      //logger.debug("Presorting by area to assist sift down effectiveness")
      val inOrder = a.children.sortBy(-_.area.area)

      OperatorContains.local().accelerateGeometry(a.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
      a.children = ListBuffer()

      val counter = new ProgressCounter(1000, Some(inOrder.size), Some(a.area.osmIds.mkString(",")))
      inOrder.foreach { b =>
        //logger.info("B: " + a.area.id + " " + b.area.area)
        //OperatorContains.local().accelerateGeometry(b.area.polygon, sr, GeometryAccelerationDegree.enumMedium)
        counter.withProgress {
          siftDown(a, b)
        }
      }

      a.children.foreach(c => {
        //Operator.deaccelerateGeometry(c.area.polygon)
      })
      //Operator.deaccelerateGeometry(a.area.polygon)

      a.children.foreach { c =>
        // logger.debug("Sifting down from " + a.area.osmIds + " to " + c.area.osmIds)
        siftDown(c)
      }
    }
  }

  def siftDown(a: GraphNode, b: GraphNode): Unit = {
    //var start = DateTime.now()
    //var siblings = a.children// .filter(c => c != b)

    //var startFilter = DateTime.now()
    val existingSiblingsWhichNewValueWouldFitIn = a.children.par.filter { s =>
        areaContains(s.area, b.area)
    }
    //val filterDuration = new Duration(startFilter, DateTime.now)
    //var secondFilterDuration: Option[Duration] = None

    if (existingSiblingsWhichNewValueWouldFitIn.nonEmpty) {
      existingSiblingsWhichNewValueWouldFitIn.foreach { s =>
        //logger.info("Added " + b.area.id + " " + b.area.fitsIn)
        // logger.debug("Found sibling which new value " + b.area.osmIds + " would fit in: " + s.area.osmIds)
        s.children = s.children :+ b.copy()
      }

    } else {
      // logger.debug("Inserting " + b.area.osmIds + " into " + a.area.osmIds)
      val geometry = b.area.polygon.copy().asInstanceOf[Polygon]
      OperatorContains.local().accelerateGeometry(geometry, sr, GeometryAccelerationDegree.enumMedium)
      a.children.append(b.copy(area = b.area.copy(polygon = geometry)))
    }

    // val duration = new Duration(start, DateTime.now)
    // logger.debug("Sift down " + siblings.size + " took " + duration.getMillis + " filter " + filterDuration.getMillis + ", second filter: " + secondFilterDuration.map(d => d.getMillis))
    Unit
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.osmIds).mkString(", ")
  }

}
