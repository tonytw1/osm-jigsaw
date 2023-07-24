package graph

import areas.{AreaComparison, PolygonCache}
import ch.hsr.geohash.GeoHash
import com.esri.core.geometry.Point
import com.google.common.cache.CacheBuilder
import model.GraphNode
import play.api.{Configuration, Logger}

import java.net.URL
import javax.inject.Inject
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GraphService @Inject()(configuration: Configuration, areasReader: AreasReader, val polygonCache: PolygonCache) extends AreaComparison {

  private val dataUrl = configuration.getString("data.url").get
  private val extractName = configuration.getString("extract.name").get

  private val geohashResolution = 3

  private val segmentCache = CacheBuilder.newBuilder()
    .maximumSize(10)
    .build[String, GraphNode]

  private val inflightReads = mutable.Map.empty[String, Future[Option[GraphNode]]]
  private val inflightReadsLock = new Object()

  def headOfGraphCoveringThisPoint(point: Point): Future[Option[GraphNode]] = {
    val geohash = GeoHash.withCharacterPrecision(point.getX, point.getY, geohashResolution)
    val cacheKey = geohash.toBase32

    Option(segmentCache.getIfPresent(cacheKey)).map { n =>
      Logger.info("Cache hit for " + cacheKey)
      Future.successful(Some(n))

    }.getOrElse {
      Logger.info("Need to load graph covering " + cacheKey)
      inflightReadsLock.synchronized {
        // Lock for an flight request we can subscribe to
        val maybeInflight: Option[Future[Option[GraphNode]]] = inflightReads.get(cacheKey)
        maybeInflight.map { inflight =>
          Logger.info("Reusing inflight request for " + cacheKey)
          inflight
        }.getOrElse {
          Logger.info("Reading graph for " + cacheKey)
          val eventualMaybeNode = loadGraphFor(point, geohash)
          inflightReads.put(cacheKey, eventualMaybeNode)
          eventualMaybeNode
        }.map { maybeLoaded =>
          inflightReads.remove(cacheKey)
          maybeLoaded.foreach { n =>
            // Cache this; makes more sense which segmented
            segmentCache.put(cacheKey, n)
          }
          maybeLoaded
        }
      }
    }
  }

  def pathsDownTo(point: Point): Future[Seq[Seq[GraphNode]]] = {

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

    val eventualMaybeHeadOfGraph = headOfGraphCoveringThisPoint(point)
    eventualMaybeHeadOfGraph.map { maybeHead: Option[GraphNode] =>
      maybeHead.map { head =>
        val containing = nodesContaining(point, head, Seq())
        val withoutRoot = containing.map(r => r.drop(1)).filter(_.nonEmpty)
        withoutRoot

      }.getOrElse {
        Seq.empty
      }
    }
  }

  // TODO dog pile protection here
  private def loadGraphFor(point: Point, geohash: GeoHash): Future[Option[GraphNode]] = {
    Future.successful {
      val graphFileURL = if (geohashResolution > 0) {
        new URL(dataUrl + "/" + extractName + "/" + extractName + ".graphv2-" + geohash.toBase32 + ".pbf")
      } else {
        new URL(dataUrl + "/" + extractName + "/" + extractName + ".graphv2.pbf")
      }

      val areasFileURL = if (geohashResolution > 0) {
        new URL(dataUrl + "/" + extractName + "/" + extractName + ".areas-" + geohash.toBase32 + ".pbf")
      } else {
        new URL(dataUrl + "/" + extractName + "/" + extractName + ".areas.pbf")
      }

      Logger.info("Loading graph segment from " + graphFileURL + " for point " + point)
      new GraphReader(areasReader).loadGraph(graphFileURL, areasFileURL)
    }
  }

}
