import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import com.esri.core.geometry._
import graphing.GraphBuilder
import input.TestValues
import model.{Area, EntityRendering, GraphNode}
import org.scalatest.FlatSpec
import resolving.BoundingBox

class GraphBuilderSpec extends FlatSpec with TestValues with EntityRendering with BoundingBox {

  val graphBuilder = new GraphBuilder()

  val largeArea = new Polygon()
  largeArea.startPath(-10, -10)
  largeArea.lineTo(10, -10)
  largeArea.lineTo(10, 10)
  largeArea.lineTo(-10, 10)
  val large = Area(name = "Large", largeArea, boundingBoxFor(largeArea))

  val smallArea = new Polygon()
  smallArea.startPath(-1, -1)
  smallArea.lineTo(1, -1)
  smallArea.lineTo(1, 1)
  smallArea.lineTo(-1, 1)
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

}
