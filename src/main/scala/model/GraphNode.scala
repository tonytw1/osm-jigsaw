package model

case class GraphNode(area: Area, var children: Set[GraphNode] = Set()) {

  def insert(areas: Seq[Area]) = {
    areas.foreach { a =>
      val newNode = GraphNode(a)
      children = children + newNode
    }
  }

  override def hashCode(): Int = area.id.hashCode()

}
