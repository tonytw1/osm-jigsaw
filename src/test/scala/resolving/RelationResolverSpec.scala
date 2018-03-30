package resolving

import input.TestValues
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec

import scala.collection.mutable

class RelationResolverSpec extends FlatSpec with TestValues with LoadTestEntities {

  val relationResolver = new RelationResolver()

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

  "relation resolver" should "make areas from relations" in {
    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head

    val areas = relationResolver.resolveAreas(Set(richmond), relationsMap, ways, nodes)

    assert(areas.size == 1)
  }

}
