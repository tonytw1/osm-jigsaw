package model

case class GraphNode(area: Area, parent: Option[GraphNode], var children: Set[GraphNode] = Set()) {

  override def hashCode(): Int = area.hashCode()

  override def toString: String = "TODO"

  def insert(newArea: Area): GraphNode = {
    val newNode = GraphNode(newArea, Some(this))
    children = children + newNode
    newNode
  }

  def render(): String = {
    area.name + parent.map(p => " / " + p.render()).getOrElse("")
  }

}
