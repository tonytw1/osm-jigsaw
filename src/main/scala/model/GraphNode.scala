package model

import java.util.concurrent.atomic.AtomicLong

case class GraphNode(area: Area, id: Long = GraphNodeIdSequence.nextId, var children: Set[GraphNode] = Set()) {

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

object GraphNodeIdSequence {

  val seq: AtomicLong = new AtomicLong(1L)

  def nextId: Long = {
    seq.getAndIncrement()
  }

}
