package graph

import java.net.URL

import areas.AreaComparison
import com.esri.core.geometry.Point
import javax.inject.Inject
import model.{GraphNode, OsmId}
import play.api.Configuration
import tags.TagService
import ch.hsr.geohash.GeoHash

class GraphService @Inject()(configuration: Configuration, tagService: TagService, areasReader: AreasReader) extends AreaComparison {

  val areasFile = new URL(configuration.getString("areas.url").get)
  val graphFile = new URL(configuration.getString("graph.url").get)
  val geohashCharacters = 4

  def headOfGraphCoveringThisPoint(point: Point) = {
    val geohash = GeoHash.withCharacterPrecision(point.getX, point.getY, geohashCharacters)
    val segmentURL = new URL(graphFile.toString + "." + geohash.toBase32)
    new GraphReader(areasReader).loadGraph(areasFile, segmentURL)
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

    val containing = nodesContaining(pt, headOfGraphCoveringThisPoint(pt), Seq())
    val withoutRoot = containing.map(r => r.drop(1)).filter(_.nonEmpty)
    withoutRoot
  }

  def tagsFor(osmId: OsmId): Option[Map[String, String]] = {
    tagService.tagsFor(osmId)
  }

}
