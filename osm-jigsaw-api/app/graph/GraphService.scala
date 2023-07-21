package graph

import areas.{AreaComparison, PolygonCache}
import ch.hsr.geohash.GeoHash
import com.esri.core.geometry.Point
import com.google.common.cache.CacheBuilder
import model.GraphNode
import play.api.{Configuration, Logger}

import java.net.URL
import javax.inject.Inject

class GraphService @Inject()(configuration: Configuration, areasReader: AreasReader, val polygonCache: PolygonCache) extends AreaComparison {

  val geohashResolution = 2

  val segmentCache = CacheBuilder.newBuilder()
    .maximumSize(10)
    .build[String, GraphNode]


  def headOfGraphCoveringThisPoint(point: Point): Option[GraphNode] = {
    val geohash = GeoHash.withCharacterPrecision(point.getX, point.getY, geohashResolution)

    val dataUrl = configuration.getString("data.url").get
    val extractName = configuration.getString("extract.name").get

    val graphFileURL = if (geohashResolution > 0 ) {
      new URL(dataUrl + "/" + extractName + "/" + extractName + ".graphv2-" + geohash.toBase32 + ".pbf")
    } else {
      new URL(dataUrl + "/" + extractName + "/" + extractName + ".graphv2.pbf")
    }

    val areasFileURL = if (geohashResolution > 0) {
      new URL(dataUrl + "/" + extractName + "/" + extractName + ".areas-" + geohash.toBase32 + ".pbf")
    } else {
      new URL(dataUrl + "/" + extractName + "/" + extractName + ".areas.pbf")
    }

    val cacheKey = graphFileURL.toExternalForm
    val cached = segmentCache.getIfPresent(cacheKey)
    Option(cached).map { n =>
      Logger.info("Cache hit for " + cacheKey)
      Some(n)

    }.getOrElse {
      Logger.info("Loading graph segment from " + graphFileURL + " for point " + point)
      val maybeNode = new GraphReader(areasReader).loadGraph(graphFileURL, areasFileURL)
      maybeNode.foreach { n =>
        // Cache this; makes more sense which segmented
        val cacheKey = graphFileURL.toExternalForm
        segmentCache.put(cacheKey, n)
      }
      maybeNode
    }
  }

  def pathsDownTo(pt: Point): Seq[Seq[GraphNode]] = {

    def nodesContaining(pt: Point, node: GraphNode, stack: Seq[GraphNode]): Seq[Seq[GraphNode]] = {
      val matchingChildren = node.children.filter { c =>
        areaContainsPoint(c, pt)
      }

      if (matchingChildren.nonEmpty) {
        matchingChildren.flatMap { m =>
          nodesContaining(pt, m, stack :+ node)
        }
      } else {
        Seq(stack :+ node)
      }
    }

    headOfGraphCoveringThisPoint(pt).map { head =>
      val containing = nodesContaining(pt, head, Seq())
      val withoutRoot = containing.map(r => r.drop(1)).filter(_.nonEmpty)
      withoutRoot

    }.getOrElse {
      Seq.empty
    }
  }

}
