package resolving

import model.{Area, EntityRendering}
import org.openstreetmap.osmosis.core.domain.v0_6.Relation
import progress.ProgressCounter

class RelationResolver extends EntityRendering with BoundingBox with PolygonBuilding {

  val outerNodeMapper = new OutlineBuilder()

  def resolveAreas(relations: Set[Relation], allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Set[Area] = {

    def resolveRelation(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, model.Way], nodes: Map[Long, (Double, Double)]): Seq[Area] = {
      val outerNodes = outerNodeMapper.outlineNodesFor(r, allRelations, ways, nodes)
      val areas = outerNodes.map { on =>
        val outerPoints = on._3.map(n => (n._1, n._2))
        areaForPoints(outerPoints).map { a =>
          val areaName = on._1
          Area(areaName, a, boundingBoxFor(a), Some(on._2))
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