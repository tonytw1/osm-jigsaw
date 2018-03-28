import java.io.{FileInputStream, ObjectInputStream}

import com.esri.core.geometry._
import graphing.GraphReader
import input.TestValues
import model.{EntityRendering, GraphNode}
import org.scalatest.FlatSpec

import scala.collection.mutable

class GraphCorrectnessSpec extends FlatSpec with TestValues with EntityRendering {

  val sr = SpatialReference.create(1)

  "graph" should "not contain duplicate nodes" in {
    fail
  }

}
