package resolving

import com.esri.core.geometry.{Point, Polygon}
import org.openstreetmap.osmosis.core.domain.v0_6.{Node, Relation, Way}

class RelationResolver {

  val outerNodeMapper = new OutlineBuilder()

  def resolve(relations: Set[Relation], allRelations: Map[Long, Relation], ways: Map[Long, Way], nodes: Map[Long, Node]): Set[(Relation, Polygon)] = {

    def resolveRelation(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, Way], nodes: Map[Long, Node]) = {


      val outerNodes = outerNodeMapper.outlineNodesFor(r, allRelations, ways, nodes)
      outerNodes.headOption.map { h =>
        val area = new Polygon()
        area.startPath(h.getLatitude, h.getLongitude)
        outerNodes.drop(1).map { on =>
          val pt = new Point(on.getLatitude, on.getLongitude)
          area.lineTo(pt)
        }
        (r, area)
      }
    }

    relations.toSeq.map { r =>
      resolveRelation(r, allRelations, ways, nodes)
    }.flatten.toSet
  }

}
