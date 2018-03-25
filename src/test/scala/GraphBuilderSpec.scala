import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import com.esri.core.geometry._
import input.TestValues
import model.{Area, EntityRendering, GraphNode}
import org.scalatest.FlatSpec

class GraphBuilderSpec extends FlatSpec with TestValues with EntityRendering {

  "geocode" should "build readable place names for point locations" in {

    var areasOutputFile = "/tmp/areas.ser"
    var graphOutputFile = "/tmp/graph.ser"

    val ois = new ObjectInputStream(new FileInputStream(areasOutputFile))
    val areas = ois.readObject.asInstanceOf[Set[Area]]
    ois.close

    val earthArea = new Polygon()
    earthArea.startPath(-180, -90)
    earthArea.lineTo(180, -90)
    earthArea.lineTo(180, 90)
    earthArea.lineTo(-180, 90)
    val earth = Area(name = "Earth", earthArea)
    var head = GraphNode(earth, None)

    var i = 0
    var j = 0
    areas.map { a =>
      head.insert(a)

      i = i + 1
      j = j + 1
      if (j == 100) {
        println(i)
        j = 0;
      }
    }

    def dump(node: GraphNode): Unit = {
      if (node.children.nonEmpty) {
        node.children.map { c =>
          dump(c)
        }
      }
      println(node.render())
    }

    println("_________________")
    dump(head)

    // Dump graph to disk
    val oos = new ObjectOutputStream(new FileOutputStream(graphOutputFile))
    oos.writeObject(head)
    oos.close
    println("Dumped graph to file: " + graphOutputFile)
    succeed
  }

}
