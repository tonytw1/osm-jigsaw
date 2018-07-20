package resolving

import areas.AreaComparison
import model.{Area, AreaIdSequence, EntityRendering}
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Relation, Way}
import progress.ProgressCounter

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

class AreaResolver extends EntityRendering with BoundingBox with PolygonBuilding with WayJoining with Logging with AreaComparison {

  val outerNodeMapper = new OutlineBuilder()

  def resolveAreas(entities: Iterable[Entity], allRelations: Map[Long, Relation], wayResolver: WayResolver, nodeResolver: NodeResolver, callback: Seq[Area] => Unit): Unit = {

    def osmIdFor(e: Entity): String = {
      e.getId.toString + e.getType.toString.take(1)
    }

    def resolveAreasForEntity(e: Entity, allRelations: Map[Long, Relation], wayResolver: WayResolver): Seq[Area] = {
      e match {
        case r: Relation =>
          val outerRings = outerNodeMapper.outlineRings(r, allRelations, wayResolver)

          outerRings.flatMap { outerRingWays =>
            val outerPoints = nodesFor(outerRingWays).flatMap(nid => nodeResolver.resolvePointForNode(nid))
            areaForPoints(outerPoints).map { p =>
              Area(AreaIdSequence.nextId, p, boundingBoxFor(p), ListBuffer(osmIdFor(r)), areaOf(p))
            }
          }

        case w: Way =>
          val osmId = osmIdFor(w)

          val isClosed = w.isClosed
          val resolvedArea = if (isClosed) {
            val outerPoints = w.getWayNodes.asScala.flatMap(nid => nodeResolver.resolvePointForNode(nid.getNodeId))
            areaForPoints(outerPoints).map { p =>
              Area(AreaIdSequence.nextId, p, boundingBoxFor(p), ListBuffer(osmId), areaOf(p))
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