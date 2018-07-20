package graphing

import input.TestValues
import model.{Area, AreaIdSequence, EntityRendering}
import org.scalatest.FlatSpec
import resolving.{BoundingBox, PolygonBuilding}

import scala.collection.mutable.ListBuffer

class GraphBuilderSpec extends FlatSpec with TestValues with EntityRendering with BoundingBox with PolygonBuilding {

  val graphBuilder = new GraphBuilder()

  val earth = makeArea("Earth", (-180, 90), (180, -90))

  val large = makeArea("Large", (-10, 10), (10, -10))
  val medium = makeArea("Medium", (-2, 2), (2, -2))
  val small = makeArea("Small", (-1, 1), (1, -1))

  val left = makeArea("Left", (-10, 10), (0, -10))
  val right = makeArea("Right", (0, 10), (10, -10))

  val overlapping = makeArea("Overlapping", (-5, 10), (5, -10))
  val fitsInLeftAndOverlapping = makeArea("Fits", (-1, 1), (0, 0))

  "graph builder" should "provide empty head node" in {
    val empty = graphBuilder.buildGraph(earth, Seq())

    assert(empty.area.osmIds.head == "Earth")
    assert(empty.children.size == 0)
  }

  "graph builder" should "insert nodes as children of head" in {
    val graph = graphBuilder.buildGraph(earth, Seq(large))

    assert(graph.children.size == 1)
  }

  "graph builder" should "place non overlapping areas at the same level" in {
    val graph = graphBuilder.buildGraph(earth, Seq(large, left, right))

    assert(graph.children.size == 1)
    var largeNode = graph.children.find(c => c.area.id == large.id).head
    assert(largeNode.area.osmIds.head == "Large")
    assert(largeNode.children.size == 2)

    assert(largeNode.children.find(c => c.area.id == left.id).head.area.osmIds.head == "Left")
    assert(largeNode.children.find(c => c.area.id == right.id).head.area.osmIds.head == "Right")
  }

  "graph builder" should "sift new nodes down into enclosing siblings" in {
    val graph = graphBuilder.buildGraph(earth, Seq(large, small))

    assert(graph.children.size == 1)
    assert(graph.children.head.area.osmIds.head == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.osmIds.head == "Small")
  }

  "graph builder" should "sift existing nodes down into enclosing siblings which are inserted after them" in {
    val graph = graphBuilder.buildGraph(earth, Seq(small, large))

    assert(graph.children.size == 1)
    assert(graph.children.head.area.osmIds.head == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.osmIds.head == "Small")
  }

  "graph builder" should "trickle down" in {
    val graph = graphBuilder.buildGraph(earth, Seq(large, medium, small))

    assert(graph.children.size == 1)
    assert(graph.children.head.area.osmIds.head == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.osmIds.head == "Medium")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.children.head.area.osmIds.head == "Small")
  }

  "graph builder" should "insertion order should not effect trickle down outcome" in {
    val graph = graphBuilder.buildGraph(earth, Seq(small, medium, large))

    assert(graph.children.size == 1)
    assert(graph.children.head.area.osmIds.head == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.osmIds.head == "Medium")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.children.head.area.osmIds.head == "Small")
  }

  "graph builder" should "items which fit inside overlapping siblings should become children of both of the overlapping regions" in {
    val sorted = graphBuilder.buildGraph(earth, Seq(left, overlapping, fitsInLeftAndOverlapping))
    val leftNode = sorted.children.find(c => c.area.id == left.id).head
    val overlappingNode = sorted.children.find(c => c.area.id == overlapping.id).head

    assert(leftNode.children.head.area.osmIds.head == "Fits")
    assert(overlappingNode.children.head.area.osmIds.head == "Fits")
  }

  def makeArea(name: String, topLeft: (Int, Int), bottomRight: (Int, Int)): Area = {
    val polygon = makePolygon(topLeft, bottomRight)
    Area(AreaIdSequence.nextId, osmIds = ListBuffer(name), polygon = polygon, boundingBox = boundingBoxFor(polygon), area = 0)
  }

}
