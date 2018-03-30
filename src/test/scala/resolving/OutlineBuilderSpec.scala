package resolving

import input.TestValues
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Node, Relation, Way}
import org.scalatest.FlatSpec

import scala.collection.mutable

class OutlineBuilderSpec extends FlatSpec with TestValues with LoadTestEntities {

  val outlineBuilder = new OutlineBuilder()

  "outline builder" should "assemble the outer ways of a relation into a consecutive list of points" in {
    val entities = loadTestData()

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
    val ways = ws.map { i => (i.getId, i)}.toMap
    val nodes = ns.map { i => (i.getId, (i.getId, i.getLatitude, i.getLongitude))}.toMap

    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head
    val outline = outlineBuilder.outlineNodesFor(richmond, relations.map(r => r.getId -> r).toMap, ways, nodes)

    assert(outline.size == 1671)
  }

  def loadTestData(): Seq[Entity] = {
    def all(entity: Entity): Boolean = true // TODO how to inline a closure in Scala?
    loadEntities("gb-test-data.pbf", all)
  }

}
