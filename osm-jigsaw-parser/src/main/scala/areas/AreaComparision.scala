package areas

import com.esri.core.geometry.{OperatorContains, Polygon, SpatialReference}
import model.Area

trait AreaComparison {

  val sr: SpatialReference = SpatialReference.create(1) // TODO wkid 1 is fast be we don't know what it is
  // Handling of the 180th meridian in OSM; https://wiki.openstreetmap.org/wiki/180th_meridian
  // We probably have areas which same this blocked on the top level.

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
