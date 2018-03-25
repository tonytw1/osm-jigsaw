package resolving

import com.esri.core.geometry.{Point, Polygon}
import model.{Area, EntityRendering}
import org.openstreetmap.osmosis.core.domain.v0_6.{Node, Relation, Way}

class RelationResolver extends EntityRendering {

  val outerNodeMapper = new OutlineBuilder()

  def resolve(relations: Set[Relation], allRelations: Map[Long, Relation], ways: Map[Long, Way], nodes: Map[Long, Node]): Set[Area] = {

    def resolveRelation(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, Way], nodes: Map[Long, Node]): Option[Area] = {

      val outerNodes = outerNodeMapper.outlineNodesFor(r, allRelations, ways, nodes)

      outerNodes.headOption.map { h =>
        val area = new Polygon()
        area.startPath(h.getLatitude, h.getLongitude)
        outerNodes.drop(1).map { on =>
          val pt = new Point(on.getLatitude, on.getLongitude)
          area.lineTo(pt)
        }
        Area(render(r), area)
      }
    }

    var i = 0L
    var j = 0

    relations.flatMap { r =>
      i = i + 1
      j = j + 1
      if (j == 1000) {
        j = 0
        println(i)
      }

      resolveRelation(r, allRelations, ways, nodes)
    }
  }

}
