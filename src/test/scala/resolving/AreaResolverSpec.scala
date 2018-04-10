package resolving

import input.TestValues
import model.{Area, EntityRendering}
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec

import scala.collection.mutable
import scala.collection.JavaConverters._

class AreaResolverSpec extends FlatSpec with TestValues with LoadTestEntities with EntityRendering {

  val areaResolver = new AreaResolver()

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
    val ways = ws.map(w => w.getId -> model.Way(w.getId, nameFor(w), w.getWayNodes.asScala.map(wn => wn.getNodeId))).toMap
    val nodes = ns.map { i => (i.getId, (i.getLatitude, i.getLongitude)) }.toMap
    val relationsMap = relations.map(r => r.getId -> r).toMap

    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head

    val areas = areaResolver.resolveAreas(Set(richmond), relationsMap, ways, nodes)

    assert(areas.size == 1)



    val bournemouth = relations.find(r => r.getId == BOURNEMOUTH._1).head
    val holdenhurst = relations.find(r => r.getId == HOLDENHURST_VILLAGE._1).head

    println(bournemouth)
    println(holdenhurst)

    val bournemouthAreas = areaResolver.resolveAreas(Set(bournemouth), relationsMap, ways, nodes)
    val holdenhurstAreas = areaResolver.resolveAreas(Set(holdenhurst), relationsMap, ways, nodes)

    println(bournemouthAreas.size)
    println(holdenhurstAreas.size)

    val bournemouthArea = bournemouthAreas.head
    val holdenhurstArea = holdenhurstAreas.head

    println(bournemouthArea)
    println(holdenhurstArea)
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
    val ways = ws.map(w => w.getId -> model.Way(w.getId, nameFor(w), w.getWayNodes.asScala.map(wn => wn.getNodeId))).toMap
    val nodes = ns.map { i => (i.getId, (i.getLatitude, i.getLongitude)) }.toMap
    val relationsMap = relations.map(r => r.getId -> r).toMap

    val newYorkCity = relations.find(r => r.getId == NEW_YORK_CITY._1).head

    val areas = areaResolver.resolveAreas(Set(newYorkCity), relationsMap, ways, nodes)

    assert(areas.size == 3)
  }

  "area resolver"should "resolve new zealand" in {
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
    val ways = ws.map(w => w.getId -> model.Way(w.getId, nameFor(w), w.getWayNodes.asScala.map(wn => wn.getNodeId))).toMap
    val nodes = ns.map { i => (i.getId, (i.getLatitude, i.getLongitude)) }.toMap
    val relationsMap = relations.map(r => r.getId -> r).toMap

    val newZealand = relations.find(r => r.getId == NEW_ZEALAND._1).head

    val areas: Set[Area] = areaResolver.resolveAreas(Set(newZealand), relationsMap, ways, nodes)

    assert(areas.size == 4)
  }

}
