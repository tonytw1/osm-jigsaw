package resolving

import input.TestValues
import model.EntityRendering
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec

import scala.collection.JavaConverters._
import scala.collection.mutable

class AreaResolverSpec extends FlatSpec with TestValues with LoadTestEntities with EntityRendering {

  val areaResolver = new AreaResolver()

  var collection: mutable.ListBuffer[ResolvedArea] = mutable.ListBuffer()

  def collectResolvedAreas(resolvedAreas: Seq[ResolvedArea]): Unit = {
    collection ++= resolvedAreas
  }

  "area resolver" should "make areas from relations" in {
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

    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head

    collection = mutable.ListBuffer[ResolvedArea]()
    val wayResolver = new InMemoryWayResolver(ways)

    areaResolver.resolveAreas(Set(richmond), relationsMap, wayResolver, collectResolvedAreas)

    assert(collection.size == 1)
  }


  // TODO assert ignores unclosed ways
  "area resolver"should "include closed loop outer ways which are part of the relation" in {
    val entities = loadEntities("new-york-city.pbf")

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
    val nodes = ns.map(i => (i.getId, (i.getLatitude, i.getLongitude))).toMap
    val relationsMap = relations.map(r => r.getId -> r).toMap

    val newYorkCity = relations.find(r => r.getId == NEW_YORK_CITY._1).head

    collection = mutable.ListBuffer[ResolvedArea]()
    val wayResolver = new InMemoryWayResolver(ways)

    val areas = areaResolver.resolveAreas(Set(newYorkCity), relationsMap, wayResolver, collectResolvedAreas)

    assert(collection.size == 3)
  }

  "area resolver" should "resolve new zealand" in { // TODO remind us again why this is special?s
    val entities = loadEntities("new-zealand.pbf")

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

    val newZealand = relations.find(r => r.getId == NEW_ZEALAND._1).head

    collection = mutable.ListBuffer[ResolvedArea]()
    val wayResolver = new InMemoryWayResolver(ways)

    val areas = areaResolver.resolveAreas(Set(newZealand), relationsMap, wayResolver, collectResolvedAreas)

    assert(collection.size == 4)
  }

}
