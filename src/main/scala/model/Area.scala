package model

import com.esri.core.geometry.Polygon

case class Area(id: Long, name: String, polygon: Polygon, boundingBox: (Double, Double, Double, Double), osmId: Option[String] = None)
