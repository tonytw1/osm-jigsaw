import java.io.{FileInputStream, ObjectInputStream}

import com.esri.core.geometry._
import input.TestValues
import model.{Area, EntityRendering}
import org.scalatest.FlatSpec

class GraphBuilderSpec extends FlatSpec with TestValues with EntityRendering {

  "geocode" should "build readable place names for point locations" in {

    var areasOutputFile = "/tmp/areas.ser"

    val ois = new ObjectInputStream(new FileInputStream(areasOutputFile))
    val areas = ois.readObject.asInstanceOf[Set[Area]]
    ois.close

    val first = areas.find(a => a.name == "United Kingdom").get
    println(first)
    val withOutFirst = areas - first

    val head = GraphNode(first, None)
    println(head)

    var i = 0
    var j = 0
    withOutFirst.map { a =>
      head.insert(a)
      i = i + 1
      j = j + 1
      if (j == 100) {
        println(i)
        j = 0;
      }


    }

    println(head.render())


    def meh(node: GraphNode): Unit = {
      if (node.children.nonEmpty) {
        node.children.map { c =>
          meh(c)
        }
      }
      println(node.render())
    }

    println("_________________")
    meh(head)

    succeed
  }

  val sr = SpatialReference.create(1)

  case class GraphNode(area: Area, parent: Option[GraphNode], var children: Set[GraphNode] = Set()) {

    override def hashCode(): Int = area.hashCode()

    def insert(value: Area): Unit = {
      val contains = OperatorContains.local().execute(area.polygon, value.polygon, sr, null)
      if (contains) {
        // println(this.area.name + " contains " + value.name)

        val childWhoEncloses = children.find { c =>
          val contains = OperatorContains.local().execute(c.area.polygon, value.polygon, sr, null)
          contains
        }

        childWhoEncloses.map { ec =>
          // println("Sifting " + value.name + " down to child: " + c.area.name)
          ec.insert(value)

        }.getOrElse {
          // println("Inserting " + value.name + " into: " + this.area.name)
          children = children.+(GraphNode(value, Some(this)))
        }
      }
    }

    def render(): String = {
      area.name + parent.map(p => " / " + p.render()).getOrElse("")
    }


  }


}
