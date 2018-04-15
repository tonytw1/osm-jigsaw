package areas

import com.esri.core.geometry.Geometry.GeometryAccelerationDegree
import com.esri.core.geometry.OperatorContains
import input.TestValues
import model.{EntityRendering, GraphNode}
import org.joda.time.{DateTime, Duration}
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec
import resolving.{AreaResolver, LoadTestEntities}

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
    val ways = ws.map(w => w.getId -> model.Way(w.getId, nameFor(w), w.getWayNodes.asScala.map(wn => wn.getNodeId))).toMap
    val nodes = ns.map { i => (i.getId, (i.getLatitude, i.getLongitude)) }.toMap
    val relationsMap = relations.map(r => r.getId -> r).toMap

    val bournemouth = relations.find(r => r.getId == BOURNEMOUTH._1).head
    val holdenhurst = relations.find(r => r.getId == HOLDENHURST_VILLAGE._1).head
    val richmond = relations.find(r => r.getId == LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION._1).head

    val bournemouthAreas = areaResolver.resolveAreas(Set(bournemouth), relationsMap, ways, nodes)
    val holdenhurstAreas = areaResolver.resolveAreas(Set(holdenhurst), relationsMap, ways, nodes)
    val richmondAreas = areaResolver.resolveAreas(Set(richmond), relationsMap, ways, nodes)

    val bournemouthArea = bournemouthAreas.head
    val holdenhurstArea = holdenhurstAreas.head
    val richmondArea = richmondAreas.head


    var  i = 0
    var node = new GraphNode(richmondArea)

    var local: OperatorContains = OperatorContains.local()
    local.accelerateGeometry(holdenhurstArea.polygon, sr, GeometryAccelerationDegree.enumMedium)
    local.accelerateGeometry(bournemouthArea.polygon, sr, GeometryAccelerationDegree.enumMedium)

     var start = DateTime.now()

    while(i < 10000) {
      assert(test(bournemouthArea, holdenhurstArea, local) == true)
      i = i + 1
    }
    var duration = new Duration(start, DateTime.now)
    println(duration.getMillis)
  }

}
