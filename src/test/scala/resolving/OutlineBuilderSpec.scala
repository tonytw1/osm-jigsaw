package resolving

import input.TestValues
import model.EntityRendering
import org.openstreetmap.osmosis.core.domain.v0_6.{Node, Relation, Way}
import org.scalatest.FlatSpec

import scala.collection.mutable
import scala.collection.JavaConverters._

class OutlineBuilderSpec extends FlatSpec with TestValues with LoadTestEntities with EntityRendering {

  val outlineBuilder = new OutlineBuilder()

  val entities = loadEntities("gb-test-data.pbf")

  val rs = mutable.Set[Relation]()
  val ws = mutable.Set[Way]()
  val ns = mutable.Set[Node]()

  entities.map {
    case r: Relation => rs.+=(r)
    case w: Way => ws.+=(w)
    case n: Node => ns.+=(n)
    case _ =>
  }

  val relations = rs.toSet
  val ways = ws.map(i => (i.getId, (i.getId + "Way", render(i), i.getWayNodes.asScala.map(_.getNodeId)))).toMap
  val nodes = ns.map { i => (i.getId, (i.getLatitude, i.getLongitude)) }.toMap
  val relationsMap = relations.map(r => r.getId -> r).toMap

  "outline builder" should "assemble the outer ways of a relation into a consecutive list of points" in {
    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head

    val outline = outlineBuilder.outlineNodesFor(richmond, relationsMap, ways, nodes)

    assert(outline.size == 1671)
  }

  "outline builder" should "account for subarea relation when building the outline of a relation" in {
    val bournemouth = relations.find(r => r.getId == BOURNEMOUTH._1).head

    val outline = outlineBuilder.outlineNodesFor(bournemouth, relationsMap, ways, nodes)

    assert(outline.size == 0)
  }

}
