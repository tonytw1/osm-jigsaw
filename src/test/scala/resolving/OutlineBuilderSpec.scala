package resolving

import input.TestValues
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Node, Relation, Way}
import org.scalatest.FlatSpec

class OutlineBuilderSpec extends FlatSpec with TestValues with LoadTestEntities {

  val outlineBuilder = new OutlineBuilder()

  "outline builder" should "assemble the outer ways of a relation into a consecutive list of points" in {
    val entities = loadTestData()

    val relations = entities.flatMap { e =>
      e match {
        case r: Relation => Some(r)
        case _ => None
      }
    }.toSet

    val ways: Map[Long, Way] = entities.flatMap { e =>
      e match {
        case w: Way => Some(w)
        case _ => None
      }
    }.map { i =>
      (i.getId, i)
    }.toMap

    val nodes = entities.flatMap { e =>
      e match {
        case n: Node => Some(n)
        case _ => None
      }
    }.map { i =>
      (i.getId, (i.getId, i.getLatitude, i.getLongitude))
    }.toMap

    val outline = outlineBuilder.outlineNodesFor(relations.head, relations.map(r => r.getId -> r).toMap, ways, nodes)

    assert(outline.size == 1671)
  }

  def loadTestData(): Seq[Entity] = {
    def all(entity: Entity): Boolean = true // TODO how to inline a closure in Scala?
    loadEntities("richmond.pbf", all)
  }

}
