package model
import scala.collection.mutable

case class Area(id: Long, points: Seq[Point], osmIds: Seq[OsmId], area: Double) {
  override def hashCode() = id.hashCode()
}

case class Point(lat: Double, lon: Double)
case class OsmId(id: Long, `type`: Char)
case class GraphNode(area: Area, children: mutable.ListBuffer[GraphNode] = mutable.ListBuffer())

case class OutputEntity(osmId: String, name: String)
case class OutputNode(id: Long, entities: Seq[OutputEntity], children: Long, area: Double)