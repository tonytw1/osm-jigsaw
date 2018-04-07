package graphing

import com.esri.core.geometry.{OperatorContains, Polygon, SpatialReference}
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
    val siblingsWhichFitInsideNewNode = siblings.filter(c => c != b).filter { s =>
      areaContains(b.area, s.area)
    }

    if (siblingsWhichFitInsideNewNode.nonEmpty) {
      println("Found " + siblingsWhichFitInsideNewNode.size + " siblings to sift down into new value " + b.area.name + " " +
        "(" + render(siblingsWhichFitInsideNewNode) + ")")

      a.children = a.children -- siblingsWhichFitInsideNewNode
      b.children = b.children ++ siblingsWhichFitInsideNewNode  // TODO parent not set
    }

    val existingSiblingWhichNewValueWouldFitIn = a.children.filter(c => c != b).find { s =>
      areaContains(s.area, b.area)
    }

    existingSiblingWhichNewValueWouldFitIn.map { s =>
      // println("Found sibling which new value " + b.area.name + " would fit in: " + s.area.name)
      a.children = a.children - b
      s.children = s.children + b
      siftDown(s, b)  // TODO test case needed
    }

  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.name).mkString(", ")
  }

  private def areaContains(a: Area, b: Area) = {
   // if (a.boundingBox._3 < b.boundingBox._1 || a.boundingBox._1 > b.boundingBox._3 || a.boundingBox._2 < b.boundingBox._4 || a.boundingBox._4 > b.boundingBox._2) {
    //  false
    //} else {
      val r = OperatorContains.local().execute(a.polygon, b.polygon, sr, null)
   // println("!!!! " + a.name + " v " + b.name + ": " + r)
      r
    //}
  }

  def areaOf(area: Area): Double = {
    Math.abs(area.polygon.calculateArea2D())
  }

}
