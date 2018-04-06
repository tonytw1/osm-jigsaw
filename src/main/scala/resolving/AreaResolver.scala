package resolving

import model.{Area, EntityRendering}
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Relation, Way}
import progress.ProgressCounter

import scala.collection.JavaConverters._

class AreaResolver extends EntityRendering with BoundingBox with PolygonBuilding with WayJoining {

  val outerNodeMapper = new OutlineBuilder()

  def resolveAreas(entities: Set[Entity], allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Set[Area] = {

    def resolveAreasForEntity(e: Entity, allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Seq[Area] = {

      e match {
        case r: Relation =>
          val outerRings = outerNodeMapper.outlineRings(r, allRelations, ways, nodes)

          val areaName = render(r) // TODO can do better
          val osmId = Some(r.getId.toString)

          val areas = outerRings.map { ways =>
            val outerPoints: Seq[(Double, Double)] = nodesFor(ways).map(nid => nodes.get(nid).map(n => (n._1, n._2))).flatten
            areaForPoints(outerPoints).map { a =>
              Area(areaName, a, boundingBoxFor(a), osmId)
            }
          }
          areas.flatten

        case w: Way =>
          val areaName = render(w) // TODO can do better
          val osmId = Some(w.getId.toString)

          val isClosed = w.isClosed
          val resolvedArea = if (isClosed) {
            val outerPoints: Seq[(Double, Double)] = w.getWayNodes.asScala.map(nid => nodes.get(nid.getNodeId).map(n => (n._1, n._2))).flatten
            areaForPoints(outerPoints).map { a =>
              Area(areaName, a, boundingBoxFor(a), osmId)
            }
          } else {
            println("Ignoring non closed way: " + w)
            None
          }
          Seq(resolvedArea).flatten
      }
    }

    val counter = new ProgressCounter(1000)
    entities.flatMap { e =>
      counter.withProgress(resolveAreasForEntity(e, allRelations, ways, nodes))
    }
  }

}