package areas

import com.esri.core.geometry.{OperatorContains, OperatorEquals, SpatialReference, Polygon}
import model.Area

trait AreaComparison {

  val sr = SpatialReference.create(1)

  def areaContains(a: Area, b: Area): Boolean = {
    if (a.boundingBox._3 < b.boundingBox._1 || a.boundingBox._1 > b.boundingBox._3 || a.boundingBox._2 < b.boundingBox._4 || a.boundingBox._4 > b.boundingBox._2) {
      false
    } else {
      OperatorContains.local().execute(a.polygon, b.polygon, sr, null) && !OperatorContains.local().execute(b.polygon, a.polygon, sr, null)
    }
  }

  /*
  def areasOverlap(a: Area, b: Area) = {
    if (a.boundingBox._3 < b.boundingBox._1 || a.boundingBox._1 > b.boundingBox._3 || a.boundingBox._2 < b.boundingBox._4 || a.boundingBox._4 > b.boundingBox._2) {
      false
    } else {
      OperatorOverlaps.local().execute(a.polygon, b.polygon, sr, null)
    }
  }
  */

  def areaSame(a: Area, b: Area): Boolean = {
    if (a.boundingBox._3 < b.boundingBox._1 || a.boundingBox._1 > b.boundingBox._3 || a.boundingBox._2 < b.boundingBox._4 || a.boundingBox._4 > b.boundingBox._2) {
      false
    } else {
      // TODO bounding box optimisation?
      OperatorEquals.local().execute(a.polygon, b.polygon, sr, null)
    }
  }

  def areaOf(p: Polygon): Double = {
    Math.abs(p.calculateArea2D())
  }

}
