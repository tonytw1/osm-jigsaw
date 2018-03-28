package model

import com.esri.core.geometry.Polygon

case class Area(name: String, polygon: Polygon, boundingBox: (Double, Double, Double, Double), osmId: Option[String] = None)
