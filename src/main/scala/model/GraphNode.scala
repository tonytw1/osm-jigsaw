package model

import com.esri.core.geometry.{OperatorContains, Point, SpatialReference}

case class GraphNode(area: Area, parent: Option[GraphNode], var children: Set[GraphNode] = Set()) {

  val sr = SpatialReference.create(1)

  override def hashCode(): Int = area.hashCode()

  override def toString: String = "TODO"

  def contains(point: Point): Boolean = {
    OperatorContains.local().execute(area.polygon, point, sr, null)
  }

  def insert(newArea: Area): GraphNode = {

    val existingChildWhichNewValueWouldFitIn = children.find { c =>
      OperatorContains.local().execute(c.area.polygon, newArea.polygon, sr, null)
    }

    existingChildWhichNewValueWouldFitIn.map { c =>
      // println("Found existing child which new value would fit in")
      c.insert(newArea)

    }.getOrElse {
     // println("Inserted " + newArea.name + " into " + this.area.name)

      val siblingsWhichFitInsideNewValue = this.children.filter { c =>
        OperatorContains.local().execute(newArea.polygon, c.area.polygon, sr, null)
      }

      var newNode = GraphNode(newArea, Some(this))
      children = children.+(newNode)

      if (siblingsWhichFitInsideNewValue.nonEmpty) {
        println("Found " + siblingsWhichFitInsideNewValue.size + " siblings to sift down into new value " + newArea.name + " " +
          "(" + siblingsWhichFitInsideNewValue.map(s => s.area.name).mkString(", ") + ")")
        children = children.--(siblingsWhichFitInsideNewValue)
        siblingsWhichFitInsideNewValue.map { s =>
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