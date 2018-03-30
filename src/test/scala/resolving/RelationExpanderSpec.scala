package resolving

import input.TestValues
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec

import scala.collection.mutable

class RelationExpanderSpec extends FlatSpec with TestValues with LoadTestEntities {

  val relationExpander = new RelationExpander()

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
  val ways = ws.map { i => (i.getId, i) }.toMap
  val nodes = ns.map { i => (i.getId, (i.getId, i.getLatitude, i.getLongitude)) }.toMap
  val relationsMap = relations.map(r => r.getId -> r).toMap

  "relation expander" should "return the top level relation only if it has no subrelations" in {
    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head

    val expanded = relationExpander.expandRelation(richmond, relationsMap)

    assert(expanded.size == 1)
    assert(expanded.head.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1)
  }

  "relation expander" should "append the top level subrelations of the relation" in {
    val bournemouth = relations.find(r => r.getId == BOURNEMOUTH._1).head

    val expanded = relationExpander.expandRelation(bournemouth, relationsMap)

    assert(expanded.size == 2)
    assert(expanded.head.getId == BOURNEMOUTH._1)
    assert(expanded.last.getId == 3565145L)
  }

  // TODO find a recursive 2nd level example

}
