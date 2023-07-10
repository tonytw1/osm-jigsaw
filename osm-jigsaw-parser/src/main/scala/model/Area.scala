package model

import com.esri.core.geometry.{Geometry, Polygon}

import scala.collection.mutable.ListBuffer

case class Area(id: Long, polygon: Polygon, boundingBox: (Double, Double, Double, Double),
                osmIds: ListBuffer[String] = ListBuffer(), area: Double,
                var convexHull: Option[Geometry] = None)
