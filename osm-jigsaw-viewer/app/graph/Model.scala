package graph

case class Point(lat: Double, lon: Double)

case class Entity(osmId: String, name: String)

case class GraphNode(id: Long, children: Long, entities: Seq[Entity])
