package model

import java.util.concurrent.atomic.AtomicLong

case class GraphNode(area: Area, var children: Set[GraphNode] = Set()) {

  def insert(areas: Seq[Area]) = {
    areas.foreach { a =>
      val newNode = GraphNode(a)
      children = children + newNode
    }
  }

  override def hashCode(): Int = area.id.hashCode()

}

object GraphNodeIdSequence {

  val seq: AtomicLong = new AtomicLong(1L)

  def nextId: Long = {
    seq.getAndIncrement()
  }

}
