import java.io.{FileInputStream, ObjectInputStream}

import com.esri.core.geometry._
import graphing.GraphReader
import input.TestValues
import model.{EntityRendering, GraphNode}
import org.scalatest.FlatSpec

import scala.collection.mutable

class GraphGeocoderSpec extends FlatSpec with TestValues with EntityRendering {

  var graphFile = "great-britain-latest.graph.ser"
  val head = loadGraph

  "geocode" should "build readable place names for point locations" in {
    Seq(london, twickenham, bournmouth, lyndhurst, edinburgh, newport, pembroke, leeds, dublin, paris, granada, newYork, halfDome).map { location =>
      val pt = new Point(location._1, location._2)

      var pathToSmallestEnclosingArea = new GraphReader().find(pt, head, mutable.Buffer())

      println(pt + ": " + pathToSmallestEnclosingArea.reverse.map(n => n.area.name).mkString(", "))
    }

    succeed
  }

  "graph" should "be fully transversable" in {
    def renderPath(p: Seq[GraphNode]) = {
      p.map(n => n.area.name).mkString(" / ")
    }

    val output = mutable.Buffer[Seq[GraphNode]]()
    new GraphReader().all(head, output)
    println(output.size)

    output.map { p =>
      // println(renderPath(p))
    }

    output.filter(p => p.last.area.name == "Huggate").map { p =>
      println(renderPath(p) + p.last.area.osmId)
    }

    val sorted = output.sortBy(p => p.size)
    sorted.map { p =>
      println(renderPath(p))
    }
  }

  def loadGraph: GraphNode = {
    println("Loading graph")
    val ois = new ObjectInputStream(new FileInputStream(graphFile))
    val head = ois.readObject.asInstanceOf[GraphNode]
    ois.close
    println("Loaded graph")
    head
  }

}
