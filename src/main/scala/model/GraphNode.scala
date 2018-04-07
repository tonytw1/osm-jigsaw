package model

case class GraphNode(area: Area, var children: Set[GraphNode] = Set()) {

  def insert(newArea: Area): GraphNode = {
    val newNode = GraphNode(newArea)
    children = children + newNode
    newNode
  }

}
