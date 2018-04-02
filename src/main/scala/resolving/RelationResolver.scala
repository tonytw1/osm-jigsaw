package resolving

import model.{Area, EntityRendering}
import org.openstreetmap.osmosis.core.domain.v0_6.{Relation, Way}

class RelationResolver extends EntityRendering with BoundingBox with PolygonBuilding {

  val outerNodeMapper = new OutlineBuilder()

  def resolveAreas(relations: Set[Relation], allRelations: Map[Long, Relation], ways: Map[Long, Seq[Long]], nodes: Map[Long, (Double, Double)]): Set[Area] = {

    def resolveRelation(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, Seq[Long]], nodes: Map[Long, (Double, Double)]): Option[Area] = {
      val outerNodes = outerNodeMapper.outlineNodesFor(r, allRelations, ways, nodes)
      val outerPoints = outerNodes.map(n => (n._1, n._2))

      areaForPoints(outerPoints).map { a =>
        Area(render(r), a, boundingBoxFor(a), Some(r.getId + r.getType.toString))
      }
    }

    var i = 0L
    var j = 0

    val total = relations.size
    relations.flatMap { r =>
      i = i + 1
      j = j + 1
      if (j == 1000) {
        j = 0
        println(i + " / " + total)
      }

      resolveRelation(r, allRelations, ways, nodes)
    }
  }

}
