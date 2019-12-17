package model

import scala.collection.mutable.ListBuffer

case class GraphNode(area: Area, var children: Seq[GraphNode] = ListBuffer()) {

  def insert(areas: Seq[Area]) = {
    areas.foreach { a =>
      val newNode = GraphNode(a)
      children = children :+ newNode
    }
  }

  override def hashCode(): Int = area.id.hashCode()

}
