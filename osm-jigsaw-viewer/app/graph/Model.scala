package graph

import play.api.libs.json.{Json, Reads}

case class Point(lat: Double, lon: Double)
object Point {
  implicit val pr: Reads[Point] = Json.reads[Point]
}
case class Entity(osmId: String, name: String)

case class GraphNode(id: Long, children: Long, entities: Seq[Entity])
object GraphNode{
  implicit val er: Reads[Entity] = Json.reads[Entity]
  implicit val gnr: Reads[GraphNode] = Json.reads[GraphNode]
}