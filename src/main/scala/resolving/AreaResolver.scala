package resolving

import model.{Area, EntityRendering}
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Relation, Way}
import progress.ProgressCounter

import scala.collection.JavaConverters._

class AreaResolver extends EntityRendering with BoundingBox with PolygonBuilding with WayJoining with Logging {

  val outerNodeMapper = new OutlineBuilder()

  def resolveAreas(entities: Iterable[Entity], allRelations: Map[Long, Relation], wayResolver: WayResolver, nodeResolver: NodeResolver, callback: Seq[Area] => Unit): Unit = {

    def resolveAreasForEntity(e: Entity, allRelations: Map[Long, Relation], wayResolver: WayResolver): Seq[Area] = {
      e match {
        case r: Relation =>
          val outerRings = outerNodeMapper.outlineRings(r, allRelations, wayResolver)

          outerRings.map { outerRingWays =>
            val outerPoints: Seq[(Double, Double)] = nodesFor(outerRingWays).map(nid => nodeResolver.resolvePointForNode(nid)).flatten
            areaForPoints(outerPoints).map { a =>
              Area(render(r), a, boundingBoxFor(a), Some(r.getId.toString))
            }
          }.flatten

        case w: Way =>
          val areaName = render(w) // TODO can do better
          val osmId = Some(w.getId.toString)

          val isClosed = w.isClosed
          val resolvedArea = if (isClosed) {
            val outerPoints: Seq[(Double, Double)] = w.getWayNodes.asScala.map(nid => nodeResolver.resolvePointForNode(nid.getNodeId)).flatten
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
      counter.withProgress(callback(resolveAreasForEntity(e, allRelations, wayResolver)))
    }
  }

  // TODO test only - move to a test fixture
  def resolveAreas(entitiesToResolve: Iterable[Entity], allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodeResolver: NodeResolver): Set[Area] = {
    var areas = Set[Area]()

    def callback(newAreas: Seq[Area]): Unit = {
      areas = areas ++ newAreas
    }

    val wayResolver = new InMemoryWayResolver(ways)
    resolveAreas(entitiesToResolve, allRelations, wayResolver, nodeResolver, callback)
    areas
  }

}