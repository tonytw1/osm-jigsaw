package areas

import com.esri.core.geometry._
import model.Area
import org.apache.logging.log4j.scala.Logging

trait AreaComparison extends Logging {

  val sr = SpatialReference.create(1)

  def areaContains(a: Area, b: Area): Boolean = {

    val contains = OperatorContains.local().execute(a.polygon, b.polygon, sr, null) && !OperatorContains.local().execute(b.polygon, a.polygon, sr, null)
      if (contains) {
        true
      } else {
        false

        /*
        // How much do these overlapping areas overlap?
        val geometry = OperatorIntersection.local().execute(a.polygon, b.polygon, sr, null)
        val ia = geometry.calculateArea2D()
        val overlap = (ia / b.area) * 100
        logger.info(a.osmIds + " (" + a.area + ")  overlap with " + b.osmIds + " (" + b.area + ") : " + overlap)
        if (overlap > 99) { // ie. 151164R and 8796242R
          true
        } else {
          false
        }
        */

    }
  }

  def areaSame(a: Area, b: Area): Boolean = {
    val areDisjoint = OperatorDisjoint.local().execute(a.polygon, b.polygon, sr, null)
    if (areDisjoint) {
      false
    } else {
      OperatorEquals.local().execute(a.polygon, b.polygon, sr, null)        // TODO Profile bounding boxes equal optimisation?
    }
  }

  def areaOf(p: Polygon): Double = {
    Math.abs(p.calculateArea2D())
  }

}
