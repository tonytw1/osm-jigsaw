package resolving

import input.TestValues
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, EntityType, Relation}
import org.scalatest.FlatSpec

class RelationExpanderSpec extends FlatSpec with TestValues with LoadTestEntities {

  val relationWayResolver = new RelationExpander()

}
