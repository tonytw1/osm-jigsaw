package model

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class GraphNode(area: Area, var children: mutable.Set[GraphNode] = mutable.Set(), var sifted: Boolean = false) {

  def insert(nodes: Seq[GraphNode]) = {
    children = children ++ nodes
  }

  override def hashCode(): Int = area.id.hashCode()

}
