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

  "relation expander" should "include the top level relation" in {
    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head

    val expanded = relationExpander.expandRelation(richmond, relationsMap)

    assert(expanded.get.size == 1)
    assert(expanded.get.head.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1)
  }

  "relation expander" should "append the top level subrelations of the relation" in {
    val bournemouth = relations.find(r => r.getId == BOURNEMOUTH._1).head

    val expanded = relationExpander.expandRelation(bournemouth, relationsMap)

    assert(expanded.get.size == 2)
    assert(expanded.get.head.getId == BOURNEMOUTH._1)
    assert(expanded.get.last.getId == 3565145L)
  }

  "relation expander" should "reject relations which contain circular sub relations" in {
    val rs = mutable.Set[Relation]()
    loadEntities("circular.pbf").map {
      case r: Relation => {
        rs.+=(r)
      }
      case _ =>
    }

    val sheepsHeadWay = rs.find(r => r.getId == 7120700L).get
    val relationsMap = rs.map(r => r.getId -> r).toMap

    val expanded = relationExpander.expandRelation(sheepsHeadWay, relationsMap)

    assert(expanded == None)
  }

  /*
  "should return none if any sub relations fail to resolve" {
    fail  // TODO test
  }
  // TODO find a recursive 2nd level example
  */

}
