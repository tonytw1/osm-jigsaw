package resolving

import areas.AreaComparison
import model.{AreaIdSequence, EntityRendering, JoinedWay}
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Relation, Way}
import progress.ProgressCounter

import scala.collection.JavaConverters._

class AreaResolver extends EntityRendering with BoundingBox with PolygonBuilding with WayJoining with Logging with AreaComparison with EntityOsmId {

  val outerNodeMapper = new OutlineBuilder()

  def resolveAreas(entities: Iterable[Entity], allRelations: Map[Long, Relation], wayResolver: WayResolver, callback: Seq[ResolvedArea] => Unit): Unit = {

    def resolveAreasForEntity(e: Entity, allRelations: Map[Long, Relation], wayResolver: WayResolver): Seq[ResolvedArea] = {
      e match {
        case r: Relation =>
          val outlines: Seq[Seq[JoinedWay]] = outerNodeMapper.outlineRings(r, allRelations, wayResolver)
          outlines.map { outline =>
            ResolvedArea(AreaIdSequence.nextId, osmIdFor(e), outline)
          }

        case w: Way =>
          val isClosed = w.isClosed
          val resolvedArea = if (isClosed) {
            val nodeIds = w.getWayNodes.asScala.map(n => n.getNodeId())
            val mw = model.Way(w.getId, nodeIds)
            val jw = JoinedWay(mw, false)
            val outline = Seq(jw)
            Some(ResolvedArea(AreaIdSequence.nextId, osmIdFor(e), outline))

          } else {
            logger.info("Ignoring non closed way: " + w)
            None
          }
          Seq(resolvedArea).flatten
      }
    }

    val counter = new ProgressCounter(10000)
    entities.foreach { e =>
      counter.withProgress(callback(resolveAreasForEntity(e, allRelations, wayResolver)))
    }
  }

  /*
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
  */

}

case class ResolvedArea(id: Long, osmId: String, outline: Seq[JoinedWay])
