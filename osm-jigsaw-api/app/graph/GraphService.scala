package graph

import areas.AreaComparison
import ch.hsr.geohash.GeoHash
import com.esri.core.geometry.Point
import model.{GraphNode, OsmId}
import play.api.{Configuration, Logger}
import tags.TagService

import java.net.URL
import javax.inject.Inject

class GraphService @Inject()(configuration: Configuration, tagService: TagService, areasReader: AreasReader) extends AreaComparison {

  val geohashCharacters = 4

  def headOfGraphCoveringThisPoint(point: Point): Option[GraphNode] = {
    val geohash = GeoHash.withCharacterPrecision(point.getX, point.getY, geohashCharacters)

    val dataUrl = configuration.getString("data.url").get
    val extractName = configuration.getString("extract.name").get
    //val segmentURL = new URL(dataUrl + "/" + extractName + "/" + extractName + ".graph." + geohash.toBase32 + ".pbf")
    val segmentURL = new URL(dataUrl + "/" + extractName + "/" + extractName + ".graph.pbf")

    Logger.info("Loading graph segment from " + segmentURL + " for point " + point)
    new GraphReader(areasReader).loadGraph(segmentURL)
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

  def tagsFor(osmId: OsmId): Option[Map[String, String]] = {
    tagService.tagsFor(osmId)
  }

}
