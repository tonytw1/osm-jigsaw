package model

import com.esri.core.geometry.{Geometry, Polygon}

import scala.collection.mutable.ListBuffer

case class Area(id: Long, polygon: Polygon, osmIds: ListBuffer[String] = ListBuffer(),
                area: Double, hull: Option[Geometry] = None)
