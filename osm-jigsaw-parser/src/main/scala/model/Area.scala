package model

import com.esri.core.geometry.Polygon

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

case class Area(id: Long, polygon: Polygon, boundingBox: (Double, Double, Double, Double),
                osmIds: ListBuffer[String] = ListBuffer(), area: Double, fitsIn: Set[Long] = Set.empty)
