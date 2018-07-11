package areas

import com.esri.core.geometry.{OperatorContains, Point, Polygon, SpatialReference}
import com.google.common.cache.CacheBuilder
import graph.GraphNode
import play.api.Logger

trait AreaComparison extends BoundingBox {

  private val sr = SpatialReference.create(1)

  val polygonCache = CacheBuilder.newBuilder()
    .maximumSize(100000)
    .build[java.lang.Long, Polygon]

  def areaContainsPoint(node: GraphNode, pt: Point): Boolean = {

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

    def polygonForNode(node: GraphNode): Option[Polygon] = {
      val key = node.id
      Option(polygonCache.getIfPresent(key)).fold {
        Logger.info("Cache miss for area polygon: " + key)
        buildPolygonForPoints(node.points).map { p =>
          polygonCache.put(key, p)
          p
        }
      }{ c =>
        Some(c)
      }
    }

    polygonForNode(node).map { p =>
      OperatorContains.local().execute(p, pt, sr, null)
    }.getOrElse {
      Logger.warn("Area has no polygon: " + node.id)
      false
    }
  }

}
