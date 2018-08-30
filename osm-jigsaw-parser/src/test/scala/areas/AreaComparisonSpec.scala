package areas

import input.TestValues
import model.{Area, AreaIdSequence, EntityRendering}
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec
import resolving._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AreaComparisonSpec extends FlatSpec with TestValues with LoadTestEntities with EntityRendering with AreaComparison with WayJoining with PolygonBuilding with BoundingBox {

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
    val ways: Map[Long, model.Way] = ws.map(w => w.getId -> model.Way(w.getId, w.getWayNodes.asScala.map(wn => wn.getNodeId))).toMap
    val nodes: Map[Long, (Double, Double)] = ns.map { i => (i.getId, (i.getLatitude, i.getLongitude)) }.toMap
    val relationsMap: Map[Long, Relation] = relations.map(r => r.getId -> r).toMap

    val bournemouth = relations.find(r => r.getId == BOURNEMOUTH._1).head
    val holdenhurst = relations.find(r => r.getId == HOLDENHURST_VILLAGE._1).head

    val bournemouthArea = buildAreaForEntity(bournemouth, relationsMap, ways, nodes)
    val holdenhurstArea =  buildAreaForEntity(holdenhurst, relationsMap, ways, nodes)

    assert(areaContains(bournemouthArea, holdenhurstArea) == true)
    assert(areaContains(holdenhurstArea, bournemouthArea) == false)
  }

  "area comparison" should "work seamlessly at the normalised longitude boundaries" in {

    def makeArea(name: String, topLeft: (Int, Int), bottomRight: (Int, Int)): Area = {  // TODO duplication
      val polygon = makePolygon(topLeft, bottomRight)
      Area(AreaIdSequence.nextId, osmIds = ListBuffer(name), polygon = polygon, boundingBox = boundingBoxFor(polygon), area = polygon.getBoundary.calculateArea2D()) // TODO
    }

    val container = makeArea("A", (10, 170), (-10, -170))
    val inside = makeArea("B", (1, 175), (-1, 176))

    assert(areaContains(container, inside) == true)
  }

  def buildAreaForEntity(entity: Entity, relationsMap: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Area = {
    // TODO build this test fixture
    var collection: mutable.ListBuffer[ResolvedArea] = mutable.ListBuffer()

    def collectResolvedAreas(resolvedAreas: Seq[ResolvedArea]): Unit = {
      collection ++= resolvedAreas
    }

    val wayResolver = new InMemoryWayResolver(ways)

    areaResolver.resolveAreas(Set(entity), relationsMap, wayResolver, collectResolvedAreas)

    val nodeResolver = new InMemoryNodeResolver(nodes)

    val areas = collection.flatMap { ra =>
      val outerPoints: Seq[(Double, Double)] = nodeIdsFor(ra.outline).flatMap(nid => nodeResolver.resolvePointForNode(nid))
      polygonForPoints(outerPoints).map { p =>
        Area(AreaIdSequence.nextId, p, boundingBoxFor(p), ListBuffer(ra.osmId), areaOf(p))
      }
    }

    areas.head
  }

}
