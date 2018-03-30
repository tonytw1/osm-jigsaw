package graphing

import input.TestValues
import model.{Area, EntityRendering}
import org.scalatest.FlatSpec
import resolving.{BoundingBox, PolygonBuilding}

class GraphBuilderSpec extends FlatSpec with TestValues with EntityRendering with BoundingBox with PolygonBuilding {

  val graphBuilder = new GraphBuilder()

  val largeArea = makePolygon((-10, 10), (10, -10))
  val large = Area(name = "Large", largeArea, boundingBoxFor(largeArea))

  val mediumArea = makePolygon((-2, 2), (2, -2))
  val medium = Area(name = "Medium", mediumArea, boundingBoxFor(mediumArea))

  val smallArea = makePolygon((-1, 1), (1, -1))
  val small = Area(name = "Small", smallArea, boundingBoxFor(smallArea))

  "graph builder" should "provide empty head node" in {
    val empty = graphBuilder.buildGraph(Seq())

    assert(empty.area.name == "Earth")
    assert(empty.children.size == 0)
  }

  "graph builder" should "insert nodes as children of head" in {
    val graph = graphBuilder.buildGraph(Seq(large))

    assert(graph.children.size == 1)
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

  "graph builder" should "trickle up" in {
    val graph = graphBuilder.buildGraph(Seq(small, medium, large))

    new GraphReader().dump(graph)

    assert(graph.children.size == 1)
    assert(graph.children.head.area.name == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.name == "Medium")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.children.head.area.name == "Small")
  }

  "graph builder" should "trickle up2" in {
    val graph = graphBuilder.buildGraph(Seq(small, large, medium))

    new GraphReader().dump(graph)

    assert(graph.children.size == 1)
    assert(graph.children.head.area.name == "Large")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.area.name == "Medium")
    assert(graph.children.head.children.size == 1)
    assert(graph.children.head.children.head.children.head.area.name == "Small")
  }

}
