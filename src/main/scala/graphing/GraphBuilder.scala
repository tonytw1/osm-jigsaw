package graphing

import com.esri.core.geometry._
import model.{Area, GraphNode}
import resolving.{BoundingBox, PolygonBuilding}

class GraphBuilder extends BoundingBox with PolygonBuilding {

  val sr = SpatialReference.create(1)

  def buildGraph(areas: Seq[Area]): GraphNode = {
    println("Building graph from " + areas.size + " areas")
    println("Presorting by area to assist sift down effectiveness")
    var c = 0
    val sorted = areas.sortBy { a =>
      areaOf(a)
    }
    val inOrder = sorted.reverse
    println("Finished sorting")

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
        println(i + "/" + total + ": " + head.children.size)
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
      areaContains(s.area, b.area)
    }

    if (existingSiblingsWhichNewValueWouldFitIn.nonEmpty) {
      a.children = a.children - b
      existingSiblingsWhichNewValueWouldFitIn.map { s =>
        println("Found sibling which new value " + b.area.name + " would fit in: " + s.area.name)
        s.children = s.children + b
        siftDown(s, b) // TODO test case needed
      }

    } else {
      println("Inserting " + b.area.name + " into " + a.area.name)
      a.children = a.children ++ Seq(b)

      val siblingsWhichFitInsideNewNode = siblings.filter(c => c != b).filter { s =>
        areaContains(b.area, s.area)
      }

      if (siblingsWhichFitInsideNewNode.nonEmpty) {
        println("Found " + siblingsWhichFitInsideNewNode.size + " siblings to sift down into new value " + b.area.name + " " +
          "(" + render(siblingsWhichFitInsideNewNode) + ")")

        a.children = a.children -- siblingsWhichFitInsideNewNode
        b.children = b.children ++ siblingsWhichFitInsideNewNode
      }

      val siblingsWhichOverlapWithNewNode = siblings.filter(c => c != b).filter{ s =>
        areasOverlap(b.area, s.area)
      }

      /*
      siblingsWhichOverlapWithNewNode.map { s =>
        println("New area " + b.area.name + " intersects with sibling: " + s.area.name + " which has " + s.children.size + " children")

        val inOverlap = s.children.filter{ sc =>
          areaContains(b.area, sc.area)
        }

        println("Found " + inOverlap.size + " overlap children to copy to new area")
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

  private def areaContains(a: Area, b: Area): Boolean = {
   // if (a.boundingBox._3 < b.boundingBox._1 || a.boundingBox._1 > b.boundingBox._3 || a.boundingBox._2 < b.boundingBox._4 || a.boundingBox._4 > b.boundingBox._2) {
    //  false
    //} else {
      OperatorContains.local().execute(a.polygon, b.polygon, sr, null)
   // println("!!!! " + a.name + " v " + b.name + ": " + r)
    //}
  }

  private def areasOverlap(a: Area, b: Area) = {
    OperatorOverlaps.local().execute(a.polygon, b.polygon, sr, null)
  }

  def areaOf(area: Area): Double = {
    Math.abs(area.polygon.calculateArea2D())
  }

}
