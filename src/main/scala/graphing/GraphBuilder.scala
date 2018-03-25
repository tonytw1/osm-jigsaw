package graphing

import com.esri.core.geometry.{OperatorContains, Polygon, SpatialReference}
import model.{Area, GraphNode}

class GraphBuilder {

  val sr = SpatialReference.create(1)

  def buildGraph(areas: Seq[Area]): GraphNode = {

    var i = 0
    var j = 0

    def showProgress: Unit = {
      i = i + 1
      j = j + 1
      if (j == 100) {
        println(i)
        j = 0
      }
    }

    val earthArea = new Polygon()
    earthArea.startPath(-90, -180)
    earthArea.lineTo(90, -180)
    earthArea.lineTo(90, 180)
    earthArea.lineTo(-90, 180)
    val earth = Area(name = "Earth", earthArea)
    var head = GraphNode(earth, None)

    areas.map { a =>
      siftDown(head, head.insert(a))
      showProgress
    }

    head
  }

  def siftDown(a: GraphNode, b: GraphNode): Unit = {
    var siblings = a.children.filter(c => c != b)

    val siblingsWhichFitInsideNewNode = siblings.filter { c =>
      OperatorContains.local().execute(b.area.polygon, c.area.polygon, sr, null)
    }

    if (siblingsWhichFitInsideNewNode.nonEmpty) {
      println("Found " + siblingsWhichFitInsideNewNode.size + " siblings to sift down into new value " + b.area.name + " " +
        "(" + render(siblingsWhichFitInsideNewNode) + ")")

      a.children = a.children -- siblingsWhichFitInsideNewNode
      b.children = b.children ++ siblingsWhichFitInsideNewNode
    }

    val existingSiblingWhichNewValueWouldFitIn = a.children.filter(c => c != b).filter { c =>
      val r = OperatorContains.local().execute(c.area.polygon,  b.area.polygon, sr, null)
      //println(c.area + " contains " + b.area + ": " + r)
      r
    }

    existingSiblingWhichNewValueWouldFitIn.map { c =>
      // println("Found sibling which new value " + b.area.name + " would fit in: " + c.area.name)
      a.children = a.children - b
      c.children = c.children + b
      siftDown(c, b)  // TODO test case needed
    }

  }

  private def render(nodes: Set[GraphNode]): String = {
    nodes.map(s => s.area.name).mkString(", ")
  }

}