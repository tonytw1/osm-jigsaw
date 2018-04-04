package resolving

import input.TestValues
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, EntityType, Relation}
import org.scalatest.FlatSpec

class OuterWayResolverSpec extends FlatSpec with TestValues with LoadTestEntities {

  private val testDataFile = "gb-test-data.pbf"

  val outerWayResolver = new OuterWayResolver()

  "outer way resolver" should "return all outer way ids for a simple relation" in {
    val relation = loadTestEntity(LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION, testDataFile).get.asInstanceOf[Relation]
    val allRelations = Map(relation.getId -> relation)

    val outerWayIds = outerWayResolver.resolveOuterWayIdsFor(relation, allRelations)

    assert(outerWayIds.length == 14)
  }

  "outer way resolver" should "ignore subareas when resolving outer ways" in {
    val relation = loadTestEntity(BOURNEMOUTH, testDataFile).get.asInstanceOf[Relation]
    val allRelations = Map(relation.getId -> relation)

    val outerWayIds = outerWayResolver.resolveOuterWayIdsFor(relation, allRelations)

    assert(outerWayIds.size == 17)
    assert(!outerWayIds.contains(265287540L))
    assert(outerWayIds.contains(199769970L))
  }

  "outer way resolver" should "include way ids for all rings of relations with more than one ring" in {
    val relation = loadTestEntity(NEW_YORK_CITY, "new-york-city.pbf").get.asInstanceOf[Relation]
    val allRelations = Map(relation.getId -> relation)

    val outerWayIds = outerWayResolver.resolveOuterWayIdsFor(relation, allRelations)

    assert(outerWayIds.size == 60)
    assert(outerWayIds.contains(444034102L))
    assert(outerWayIds.contains(61602969L))

    // Should also include the Liberty Island way
    assert(outerWayIds.contains(4820654L))
  }

  def loadTestEntity(target: (Long, EntityType), file: String): Option[Entity] = {
    def predicate(entity: Entity): Boolean = entity.getId == target._1 && entity.getType == target._2
    loadEntities(file, predicate).headOption
  }

}
