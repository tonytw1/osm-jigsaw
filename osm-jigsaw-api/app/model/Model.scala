package model
import play.api.libs.json.{Json, OWrites}

import scala.collection.mutable

case class Area(id: Long, points: Seq[Point], osmIds: Seq[OsmId], area: Double) {
  override def hashCode() = id.hashCode()
}

case class Point(lat: Double, lon: Double)
object Point {
  implicit val pw: OWrites[Point] = Json.writes[model.Point]
}
case class OsmId(id: Long, `type`: Char)
case class GraphNode(area: Area, children: Seq[GraphNode] = mutable.ListBuffer())

case class OutputEntity(osmId: String, name: String)
case class OutputNode(id: Long, entities: Seq[OutputEntity], children: Long, area: Double)
object OutputNode{
  implicit val ew: OWrites[OutputEntity] = Json.writes[OutputEntity]
  implicit val nw: OWrites[OutputNode] = Json.writes[OutputNode]
}