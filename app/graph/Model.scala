package graph

import scala.collection.mutable

case class Point(lat: Double, lon: Double)

case class Area(id: Long, name: Option[String] = None, points: Seq[Point], osmId: Option[String])

case class GraphNode(area: Area, children: mutable.ListBuffer[GraphNode] = mutable.ListBuffer())

case class SparseArea(id: Long, name: Option[String] = None, osmId: Option[String])
