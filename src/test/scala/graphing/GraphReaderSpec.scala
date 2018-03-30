package graphing

import geocoding.LoadGraph
import input.TestValues
import model.{EntityRendering, GraphNode}
import org.scalatest.FlatSpec

import scala.collection.mutable

class GraphReaderSpec extends FlatSpec with TestValues with EntityRendering with LoadGraph {

  var graphFile = "great-britain-latest.graph.ser"
  val head = loadGraph(graphFile)

  "graph" should "be fully transversable" in {
    def renderPath(p: Seq[GraphNode]) = {
      p.map(n => n.area.name).mkString(" / ")
    }

    val output = mutable.Buffer[Seq[GraphNode]]()
    new GraphReader().all(head, output)
    println(output.size)

    output.filter(p => p.last.area.name == "Huggate").map { p =>
      println(renderPath(p) + p.last.area.osmId)
    }

    val sorted = output.sortBy(p => p.size)
    sorted.map { p =>
      println(renderPath(p))
    }
  }

}
