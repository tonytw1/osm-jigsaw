package resolving

import com.esri.core.geometry.Polygon

trait PolygonBuilding {

  def makePolygon(topLeft: (Int, Int), bottomRight: (Int, Int)): Polygon = {
    val polygon = new Polygon()
    polygon.startPath(topLeft._1, topLeft._2)
    polygon.lineTo(topLeft._1, bottomRight._2)
    polygon.lineTo(bottomRight._1, bottomRight._2)
    polygon.lineTo(bottomRight._1, topLeft._2)
    polygon
  }

}
