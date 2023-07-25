package model

import scala.collection.mutable

case class FlippedGraphNode(id: Long, children: mutable.Set[FlippedGraphNode]) {
  override def hashCode(): Int = id.hashCode()
}