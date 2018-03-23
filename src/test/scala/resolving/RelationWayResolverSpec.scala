package resolving

import input.sinks.OsmEntitySink
import input.{OsmReader, TestValues}
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Relation}
import org.scalatest.FlatSpec

class RelationWayResolverSpec extends FlatSpec with TestValues {

  val relationWayResolver = new RelationWayResolver()

  "relation resolver" should "return all outer way ids for a simple relation" in {
    val relation = loadTestEntity().get.asInstanceOf[Relation]
    println(relation)
    val allRelations = Map(relation.getId -> relation)

    val outerWayIds = relationWayResolver.resolveOuterWayIdsFor(relation, allRelations)

    assert(outerWayIds.length == 14)
  }

  def loadTestEntity(): Option[Entity] = {
    def richmond(entity: Entity): Boolean = {
      entity.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1 && entity.getType == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._2
    }

    val sink = new OsmEntitySink(richmond)
    val reader = new OsmReader("richmond.pbf", sink)
    reader.read
    sink.found.headOption
  }

}
