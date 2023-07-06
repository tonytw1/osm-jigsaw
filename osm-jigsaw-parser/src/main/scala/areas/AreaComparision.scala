package areas

import com.esri.core.geometry.{OperatorContains, OperatorEquals, Polygon, SpatialReference}
import model.Area

trait AreaComparison {

  val sr = SpatialReference.create(1)

  def areaContains(a: Area, b: Area): Boolean = {
    val contains = a.convexHull.map { ch =>
      OperatorContains.local().execute(ch, b.polygon, sr, null) &&
        OperatorContains.local().execute(a.polygon, b.polygon, sr, null)
    }.getOrElse {
      OperatorContains.local().execute(a.polygon, b.polygon, sr, null)
    }
    if (contains) {
      !OperatorContains.local().execute(b.polygon, a.polygon, sr, null) // TODO this is a duplicate area check which should not have got this far
    } else {
      false
    }
  }

  def areaSame(a: Area, b: Area): Boolean = {
    if (a.boundingBox._3 < b.boundingBox._1 || a.boundingBox._1 > b.boundingBox._3 || a.boundingBox._2 < b.boundingBox._4 || a.boundingBox._4 > b.boundingBox._2) {
      false
    } else {
      OperatorEquals.local().execute(a.polygon, b.polygon, sr, null)        // TODO Profile bounding boxes equal optimisation?
    }
  }

  def areaOf(p: Polygon): Double = {
    Math.abs(p.calculateArea2D())
  }

}
