package resolving

import input.TestValues
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, EntityType, Relation}
import org.scalatest.FlatSpec

class RelationWayResolverSpec extends FlatSpec with TestValues with LoadTestEntities {

  val relationWayResolver = new RelationWayResolver()

  "relation resolver" should "return all outer way ids for a simple relation" in {
    val relation = loadTestEntity(LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION, "richmond.pbf").get.asInstanceOf[Relation]
    val allRelations = Map(relation.getId -> relation)

    val outerWayIds = relationWayResolver.resolveOuterWayIdsFor(Seq(relation), allRelations)

    assert(outerWayIds.length == 14)
  }

  "relation resolver" should "deal with a relation with more than one ring" in {
    val relation = loadTestEntity(NEW_YORK_CITY, "new-york-city.pbf").get.asInstanceOf[Relation]
    val allRelations = Map(relation.getId -> relation)

    val outerWayIds = relationWayResolver.resolveOuterWayIdsFor(Seq(relation), allRelations)

    assert(outerWayIds.contains(444034102L))
    assert(outerWayIds.contains(61602969L))

    // Should not contain the Liberty Island way
    assert(!outerWayIds.contains(4820654L))
  }

  "relation resolver" should "deal with relation with a subarea" in {

    val relation = loadTestEntity(BOURNEMOUTH, "bournemouth.pbf").get.asInstanceOf[Relation]
    val allRelations = Map(relation.getId -> relation)

    val outerWayIds = relationWayResolver.resolveOuterWayIdsFor(Seq(relation), allRelations)
    println(outerWayIds)
    println(outerWayIds.size)

    assert(!outerWayIds.contains(265287540L))
    assert(outerWayIds.contains(199769970L))
  }

  def loadTestEntity(target: (Long, EntityType), file: String): Option[Entity] = {
    def predicate(entity: Entity): Boolean = entity.getId == target._1 && entity.getType == target._2
    loadEntities(file, predicate).headOption
  }

}
