import java.io.{FileInputStream, ObjectInputStream}

import com.esri.core.geometry._
import input.TestValues
import model.{Area, EntityRendering, GraphNode}
import org.scalatest.FlatSpec

class GraphGeocoderSpec extends FlatSpec with TestValues with EntityRendering {

  "geocode" should "build readable place names for point locations" in {

    var graphFile = "/tmp/graph.ser"

    val ois = new ObjectInputStream(new FileInputStream(graphFile))
    val head = ois.readObject.asInstanceOf[GraphNode]
    ois.close

    println(head.area.name)
    println(head.children.map(n => n.area.name).mkString(", "))

    Seq(lyndhurst).map { location =>
      val pt = new Point(location._1, location._2)

      def find(pt: Point, node: GraphNode): Option[GraphNode] = {
        val child = node.children.find(c => c.contains(pt))
        child.map { c =>
          println(c.area.name)
          find(pt, c)
        }.getOrElse{
          if (node.contains(pt)) {
            Some(node)
          } else {
            None
          }
        }
      }

      println(find(pt, head).map(n =>
        n.area.name))
    }

    succeed
  }

}
