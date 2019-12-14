package model

import com.esri.core.geometry.Polygon

import scala.collection.mutable.ListBuffer

case class Area(id: Long, polygon: Polygon, osmIds: ListBuffer[String] = ListBuffer(), area: Double)
