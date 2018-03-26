import java.io.{FileInputStream, ObjectInputStream}

import com.esri.core.geometry._
import graphing.GraphReader
import input.TestValues
import model.{Area, EntityRendering, GraphNode}
import org.joda.time.{DateTime, Duration}
import org.scalatest.FlatSpec

import scala.collection.mutable

class GraphGeocoderSpec extends FlatSpec with TestValues with EntityRendering {

  val sr = SpatialReference.create(1)

  "geocode" should "build readable place names for point locations" in {

    var graphFile = "europe.graph.ser"
    println("Loading graph")
    val ois = new ObjectInputStream(new FileInputStream(graphFile))
    val head = ois.readObject.asInstanceOf[GraphNode]
    ois.close
    println("Loaded graph")

    Seq(london, twickenham, bournmouth, lyndhurst, edinburgh, newport, pembroke, leeds, dublin, paris, granada, newYork, halfDome).map { location =>
      val pt = new Point(location._1, location._2)

      var pathToSmallestEnclosingArea = new GraphReader().find(pt, head, mutable.Buffer())

      println(pt + ": " + pathToSmallestEnclosingArea.reverse.map(n => n.area.name).mkString(", "))
    }

    succeed
  }

}
