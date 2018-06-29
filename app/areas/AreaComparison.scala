package areas

import com.esri.core.geometry.{OperatorContains, Point, Polygon, SpatialReference}
import graph.Area
import play.api.Logger

trait AreaComparison {

  private val sr = SpatialReference.create(1)

  def areaContainsPoint(area: Area, pt: Point): Boolean = {

    def buildPolygonForPoints(points: Seq[graph.Point]): Option[Polygon] = {
      points.headOption.map { n =>
        val polygon = new Polygon()
        polygon.startPath(n.lat, n.lon)
        points.drop(1).map { on =>
          polygon.lineTo(on.lat, on.lon)
        }
        polygon
      }
    }

    def polygonForArea(area: Area): Option[Polygon] = {
      buildPolygonForPoints(area.points)
    }

    polygonForArea(area).map { p =>
      OperatorContains.local().execute(p, pt, sr, null)
    }.getOrElse {
      Logger.warn("Area has no polygon: " + area.name)
      false
    }
  }

}
