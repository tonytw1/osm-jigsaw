package graphing

import input.TestValues
import model.{Area, EntityRendering, GraphNode, GraphNodeIdSequence}
import org.scalatest.FlatSpec
import resolving.{BoundingBox, PolygonBuilding}

class GraphBuilderSpec extends FlatSpec with TestValues with EntityRendering with BoundingBox with PolygonBuilding {

  val graphBuilder = new GraphBuilder()

  val large = makeArea("Large", (-10, 10), (10, -10))
  val medium = makeArea("Medium", (-2, 2), (2, -2))
  val small = makeArea("Small", (-1, 1), (1, -1))

  val left = makeArea("Left", (-10, 10), (0, -10))
  val right = makeArea("Right", (0, 10), (10, -10))

  val overlapping = makeArea("Overlapping", (-5, 10), (5, -10))
  val fitsInLeftAndOverlapping = makeArea("Fits", (-1, 1), (0, 0))

  "graph builder" should "provide empty head node" in {
    val empty = graphBuilder.buildGraph(Seq())

    assert(empty.area.name == "Earth")
    assert(empty.children.size == 0)
  }

  "graph builder" should "insert nodes as children of head" in {
    val graph = graphBuilder.buildGraph(Seq(large))

    assert(graph.children.size == 1)
  }

  "graph builder" should "place non overlapping areas at the same level" in {
    val graph = graphBuilder.buildGraph(Seq(large, left, right))

    assert(graph.children.size == 1)
    assert(graph.children.head.area.name == "Large")
    assert(graph.children.head.children.size == 2)
    assert(graph.children.head.children.head.area.name == "Left")
    assert(graph.children.head.children.last.area.name == "Right")
  }

  "graph builder" should "sift new nodes down into enclosing siblings" in {
    val graph = graphBuilder.buildGraph(Seq(large, small))

    assert(graph.children.size == 1)
    assert(graph.children.head.area.name == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.name == "Small")
  }

  "graph builder" should "sift existing nodes down into enclosing siblings which are inserted after them" in {
    val graph = graphBuilder.buildGraph(Seq(small, large))

    assert(graph.children.size == 1)
    assert(graph.children.head.area.name == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.name == "Small")
  }

  "graph builder" should "trickle down" in {
    val graph = graphBuilder.buildGraph(Seq(large, medium, small))

    assert(graph.children.size == 1)
    assert(graph.children.head.area.name == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.name == "Medium")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.children.head.area.name == "Small")
  }

  "graph builder" should "insertion order should not effect trickle down outcome" in {
    val graph = graphBuilder.buildGraph(Seq(small, medium, large))

    new GraphReader().dump(graph)

    assert(graph.children.size == 1)
    assert(graph.children.head.area.name == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.name == "Medium")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.children.head.area.name == "Small")
  }

  "graph builder" should "items which fit inside overlapping siblings should become children of all of the overlapping regions" in {
    val leftFirst: GraphNode = graphBuilder.buildGraph(Seq(large, left, overlapping, fitsInLeftAndOverlapping))
    new GraphReader().dump(leftFirst)

    val overlappingNode = leftFirst.children.head.children.head
    val leftNode = leftFirst.children.head.children.last
    println(overlapping)


    assert(overlappingNode.children.head.area.name == "Fits")
    assert(leftNode.children.head.area.name == "Fits")
    // TODO
  }

  def makeArea(name: String, topLeft: (Int, Int), bottomRight: (Int, Int)): Area = {
    val area = makePolygon(topLeft, bottomRight)
    Area(GraphNodeIdSequence.nextId, name, area, boundingBoxFor(area))
  }

}
