package resolving

import model.{Area, EntityRendering, JoinedWay}
import org.openstreetmap.osmosis.core.domain.v0_6.Relation
import progress.ProgressCounter

class RelationResolver extends EntityRendering with BoundingBox with PolygonBuilding with WayJoining {

  val outerNodeMapper = new OutlineBuilder()

  def resolveAreas(relations: Set[Relation], allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Set[Area] = {

    def resolveRelation(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Seq[Area] = {
      val outerRings = outerNodeMapper.outlineRings(r, allRelations, ways, nodes)

      val areas = outerRings.map { ways =>

        val areaName = render(r)  // TODO can do better
        val osmId = Some(r.getId.toString)

        val outerPoints: Seq[(Double, Double)] = nodesFor(ways).map(nid => nodes.get(nid).map(n => (n._1, n._2))).flatten

        areaForPoints(outerPoints).map { a =>
          Area(areaName, a, boundingBoxFor(a), osmId)
        }
      }
      areas.flatten
    }

    val counter = new ProgressCounter(1000)
    relations.flatMap { r =>
      counter.withProgress(resolveRelation(r, allRelations, ways, nodes))
    }
  }

}