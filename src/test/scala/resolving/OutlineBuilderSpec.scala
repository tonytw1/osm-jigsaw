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

  entities.map {
    case r: Relation => rs.+=(r)
    case w: Way => ws.+=(w)
    case _ =>
  }

  val relations = rs.toSet
  val ways = ws.map(w => w.getId -> model.Way(w.getId, w.getWayNodes.asScala.map(wn => wn.getNodeId))).toMap
  val relationsMap = relations.map(r => r.getId -> r).toMap

  "outline builder" should "assemble the outer ways of a relation into a consecutive list of ways" in {
    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head

    val wayResolver = new InMemoryWayResolver(ways)
    val rings = outlineBuilder.outlineRings(richmond, relationsMap, wayResolver)

    assert(rings.size == 1)
    val ring = rings.head
    assert(ring.size == 14)
  }

  "outline builder" should "account for subarea relation when building the outline of a relation" in {
    val bournemouth = relations.find(r => r.getId == BOURNEMOUTH._1).head
    val wayResolver = new InMemoryWayResolver(ways)
    val rings = outlineBuilder.outlineRings(bournemouth, relationsMap, wayResolver)

    assert(rings.size == 1)
    val ring = rings.head
    assert(ring.size == 17)

    assert(!(ring.map(w => w.way.id) contains 265287540))
  }

}
