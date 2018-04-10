package graphing

import areas.AreaComparison
import com.esri.core.geometry._
import model.{Area, GraphNode}
import org.apache.logging.log4j.scala.Logging
import resolving.{BoundingBox, PolygonBuilding}
import org.apache.logging.log4j.Level

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

    def showProgress: Unit = {
      i = i + 1
      j = j + 1
      if (j == 100) {
        logger.info(i + "/" + total + ": " + head.children.size)
        j = 0
      }
    }

    inOrder.map { a =>
      siftDown(head, head.insert(a))
      showProgress
    }

    head
  }

  def siftDown(a: GraphNode, b: GraphNode): Unit = {
    var siblings = a.children

    var filter = a.children.filter(c => c != b)
    val existingSiblingsWhichNewValueWouldFitIn = filter.filter { s =>
      val r = areaContains(s.area, b.area)
      logger.info("SD: " + b.area.name + " into " + a.area.name + " -> " + s.area.name + " contains " + b.area.name + ": " + r)
      r
    }

    if (existingSiblingsWhichNewValueWouldFitIn.nonEmpty) {
      a.children = a.children - b
      existingSiblingsWhichNewValueWouldFitIn.map { s =>
        logger.info("Found sibling which new value " + b.area.name + " would fit in: " + s.area.name)
        s.children = s.children + b
        siftDown(s, b) // TODO test case needed
      }

    } else {
      logger.info("Inserting " + b.area.name + " into " + a.area.name)
      a.children = a.children ++ Seq(b)

      val siblingsWhichFitInsideNewNode = siblings.filter(c => c != b).filter { s =>
        areaContains(b.area, s.area)
      }

      if (siblingsWhichFitInsideNewNode.nonEmpty) {
        logger.info("Found " + siblingsWhichFitInsideNewNode.size + " siblings to sift down into new value " + b.area.name + " " +
          "(" + render(siblingsWhichFitInsideNewNode) + ")")

        a.children = a.children -- siblingsWhichFitInsideNewNode
        b.children = b.children ++ siblingsWhichFitInsideNewNode
      }

      val siblingsWhichOverlapWithNewNode = siblings.filter(c => c != b).filter{ s =>
        areasOverlap(b.area, s.area)
      }

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
  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.name).mkString(", ")
  }

}
