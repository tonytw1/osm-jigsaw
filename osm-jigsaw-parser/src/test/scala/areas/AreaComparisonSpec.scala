package areas

import input.TestValues
import model.EntityRendering
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec
import resolving.{AreaResolver, InMemoryNodeResolver, LoadTestEntities, NodeResolver}

import scala.collection.JavaConverters._
import scala.collection.mutable

class AreaComparisonSpec extends FlatSpec with TestValues with LoadTestEntities with EntityRendering with AreaComparison {

  val areaResolver = new AreaResolver()

  "area comparison" should "identify when an area is contained within another area" in {
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
    val ways = ws.map(w => w.getId -> model.Way(w.getId, w.getWayNodes.asScala.map(wn => wn.getNodeId))).toMap
    val nodes = ns.map { i => (i.getId, (i.getLatitude, i.getLongitude)) }.toMap
    val relationsMap = relations.map(r => r.getId -> r).toMap

    val bournemouth = relations.find(r => r.getId == BOURNEMOUTH._1).head
    val holdenhurst = relations.find(r => r.getId == HOLDENHURST_VILLAGE._1).head

    val nodeResolver = new InMemoryNodeResolver(nodes)
    val bournemouthAreas = areaResolver.resolveAreas(Set(bournemouth), relationsMap, ways, nodeResolver)
    val holdenhurstAreas = areaResolver.resolveAreas(Set(holdenhurst), relationsMap, ways, nodeResolver)
    val bournemouthArea = bournemouthAreas.head
    val holdenhurstArea = holdenhurstAreas.head

    assert(areaContains(bournemouthArea, holdenhurstArea) == true)
    assert(areaContains(holdenhurstArea, bournemouthArea) == false)
  }

}
