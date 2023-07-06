package model

import scala.collection.mutable.ListBuffer

case class GraphNode(area: Area, var children: Seq[GraphNode] = ListBuffer()) {

  def insert(nodes: Seq[GraphNode]) = {
    children = children ++ nodes
  }

  override def hashCode(): Int = area.id.hashCode()

}
