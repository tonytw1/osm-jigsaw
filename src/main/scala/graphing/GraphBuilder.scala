package graphing

import com.esri.core.geometry.{OperatorContains, Polygon, SpatialReference}
import model.{Area, GraphNode}
import resolving.BoundingBox

class GraphBuilder extends BoundingBox {

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

    val earthArea = new Polygon()
    earthArea.startPath(-90, -180)
    earthArea.lineTo(90, -180)
    earthArea.lineTo(90, 180)
    earthArea.lineTo(-90, 180)
    val earth = Area(name = "Earth", earthArea, boundingBoxFor(earthArea))
    var head = GraphNode(earth, None)

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
    var siblings = a.children.filter(c => c != b)

    val siblingsWhichFitInsideNewNode = siblings.filter { c =>
      areaContains(b.area, c.area)
    }

    if (siblingsWhichFitInsideNewNode.nonEmpty) {
      println("Found " + siblingsWhichFitInsideNewNode.size + " siblings to sift down into new value " + b.area.name + " " +
        "(" + render(siblingsWhichFitInsideNewNode) + ")")

      a.children = a.children -- siblingsWhichFitInsideNewNode
      b.children = b.children ++ siblingsWhichFitInsideNewNode
    }

    val existingSiblingWhichNewValueWouldFitIn = a.children.filter(c => c != b).filter { c =>
      areaContains(c.area, b.area)
    }

    existingSiblingWhichNewValueWouldFitIn.map { c =>
      //println("Found sibling which new value " + b.area.name + " would fit in: " + c.area.name)
      a.children = a.children - b
      c.children = c.children + b
      siftDown(c, b)  // TODO test case needed
    }

  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.name).mkString(", ")
  }

  private def areaContains(a: Area, b: Area) = {
    if (a.boundingBox._3 < b.boundingBox._1 || a.boundingBox._1 > b.boundingBox._3 || a.boundingBox._2 < b.boundingBox._4 || a.boundingBox._4 > b.boundingBox._2) {
      false
    } else {
      OperatorContains.local().execute(a.polygon, b.polygon, sr, null)
    }
  }

  def areaOf(area: Area): Double = {
    Math.abs(area.polygon.calculateArea2D())
  }

}
