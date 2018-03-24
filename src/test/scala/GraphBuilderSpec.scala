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

    println(head.render())

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

    succeed
  }

  val sr = SpatialReference.create(1)

  case class GraphNode(area: Area, parent: Option[GraphNode], var children: Set[GraphNode] = Set()) {

    override def hashCode(): Int = area.hashCode()

    override def toString: String = "MEH"


    def insert(newArea: Area): GraphNode = {

      val existingChildWhichNewValueWouldFitIn = children.find { c =>
        OperatorContains.local().execute(c.area.polygon, newArea.polygon, sr, null)
      }

      existingChildWhichNewValueWouldFitIn.map { c =>
        // println("Found existing child which new value would fit in")
        c.insert(newArea)

      }.getOrElse {
        // println("Inserted " + newArea.name + " into " + this.area.name)

        val siblingsWhichFitInsideNewValeu = this.children.filter { c =>
          OperatorContains.local().execute(newArea.polygon, c.area.polygon, sr, null)
        }

        var newNode = GraphNode(newArea, Some(this))
        children = children.+(newNode)

        if (siblingsWhichFitInsideNewValeu.nonEmpty) {
          println("Found " + siblingsWhichFitInsideNewValeu.size + " siblings to sift down into new value " + newArea.name + " " +
            "(" + siblingsWhichFitInsideNewValeu.map(s => s.area.name).mkString(", ") + ")")
          children = children.--(siblingsWhichFitInsideNewValeu)
          siblingsWhichFitInsideNewValeu.map { s =>
            newNode.insert(s.area)
          }

        }

      }

      this
    }

    def render(): String = {
      area.name + parent.map(p => " / " + p.render()).getOrElse("")
    }

  }


}
