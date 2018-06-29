package areas

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.{OperatorContains, Point, Polygon, SpatialReference}
import com.google.common.cache.{Cache, CacheBuilder}
import graph.Area
import play.api.Logger

trait AreaComparison {

  private val sr = SpatialReference.create(1)

  val polygonCache = CacheBuilder.newBuilder()
    .maximumSize(100000)
    .build[java.lang.Long, Polygon]

  def areaContainsPoint(area: Area, pt: Point): Boolean = {

    def buildPolygonForPoints(points: Seq[graph.Point]): Option[Polygon] = {
      points.headOption.map { n =>
        val polygon = new Polygon()
        polygon.startPath(n.lat, n.lon)
        points.drop(1).map { on =>
          polygon.lineTo(on.lat, on.lon)
        }
        OperatorContains.local().accelerateGeometry(polygon, sr, GeometryAccelerationDegree.enumMedium)
        polygon
      }
    }

    def polygonForArea(area: Area): Option[Polygon] = {
      val key = area.id
      Option(polygonCache.getIfPresent(key)).fold {
        buildPolygonForPoints(area.points).map { p =>
          polygonCache.put(area.id, p)
          p
        }
      }{ c =>
        Some(c)
      }
    }

    polygonForArea(area).map { p =>
      OperatorContains.local().execute(p, pt, sr, null)
    }.getOrElse {
      Logger.warn("Area has no polygon: " + area.name)
      false
    }
  }

}
