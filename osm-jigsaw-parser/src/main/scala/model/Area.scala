package model

import com.esri.core.geometry.Polygon

import scala.collection.mutable.ListBuffer

case class Area(id: Long, outline: Seq[JoinedWay], polygon: Polygon, boundingBox: (Double, Double, Double, Double), osmIds: ListBuffer[String] = ListBuffer(), area: Double)
