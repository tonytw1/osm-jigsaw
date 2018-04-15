package model

import java.util.UUID

case class GraphNode(area: Area, var children: Set[GraphNode] = Set(), id: UUID = UUID.randomUUID()) {

  def insert(newArea: Area): GraphNode = {
    val newNode = GraphNode(newArea)
    children = children + newNode
    newNode
  }

  def insert(areas: Seq[Area]) = {
    areas.foreach { a =>
      val newNode = GraphNode(a)
      children = children + newNode
    }
  }

  override def hashCode(): Int = id.hashCode()

}
