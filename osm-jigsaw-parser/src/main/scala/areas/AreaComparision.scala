package areas

import com.esri.core.geometry.{OperatorContains, Polygon, SpatialReference}
import model.Area

trait AreaComparison {

  val sr = SpatialReference.create(1)

  def areaContains(a: Area, b: Area): Boolean = {
    OperatorContains.local().execute(a.polygon, b.polygon, sr, null)
  }

  def areaSame(a: Area, b: Area): Boolean = {
      OperatorContains.local().execute(a.polygon, b.polygon, sr, null) &&
        OperatorContains.local().execute(b.polygon, a.polygon, sr, null)
  }

  def areaOf(p: Polygon): Double = {
    Math.abs(p.calculateArea2D())
  }

}
