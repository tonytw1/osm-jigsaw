package resolving

import model.{Area, EntityRendering}
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Relation, Way}
import progress.ProgressCounter

import scala.collection.JavaConverters._

class AreaResolver extends EntityRendering with BoundingBox with PolygonBuilding with WayJoining with Logging {

  val outerNodeMapper = new OutlineBuilder()

  def resolveAreas(entities: Iterable[Entity], allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)], callback: Seq[Area] => Unit): Unit = {

    def resolveAreasForEntity(e: Entity, allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Seq[Area] = {
      e match {
        case r: Relation =>
          val outerRings = outerNodeMapper.outlineRings(r, allRelations, ways)

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
            logger.info("Ignoring non closed way: " + w)
            None
          }
          Seq(resolvedArea).flatten
      }
    }

    val counter = new ProgressCounter(1000)
    entities.foreach { e =>
      counter.withProgress(callback(resolveAreasForEntity(e, allRelations, ways, nodes)))
    }
  }

  def resolveAreas(entitiesToResolve: Iterable[Entity], allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Set[Area] = {
    var areas = Set[Area]()

    def callback(newAreas: Seq[Area]): Unit = {
      areas = areas ++ newAreas
    }

    resolveAreas(entitiesToResolve, allRelations, ways, nodes, callback)
    areas
  }

}