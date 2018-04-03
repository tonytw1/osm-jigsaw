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

  /* Boston relation is defined as a single closed way
  / EarthNone / United KingdomSome(62149Relation) / EnglandSome(58447Relation) / East MidlandsSome(151279Relation) /
  LincolnshireSome(78312Relation) / BostonSome(58553Relation) / BostonSome(2776224Relation) / 206552267WaySome(206552267WayWay)
  */

}
