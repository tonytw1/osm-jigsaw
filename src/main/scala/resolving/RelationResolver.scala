package resolving

import com.esri.core.geometry.{Point, Polygon}
import model.{Area, EntityRendering}
import org.openstreetmap.osmosis.core.domain.v0_6.{Node, Relation, Way}

class RelationResolver extends EntityRendering with BoundingBox {

  val outerNodeMapper = new OutlineBuilder()

  def resolve(relations: Set[Relation], allRelations: Map[Long, Relation], ways: Map[Long, Way], nodes: Map[Long, (Long, Double, Double)]): Set[Area] = {

    def resolveRelation(r: Relation, allRelations: Map[Long, Relation], ways: Map[Long, Way], nodes: Map[Long, (Long, Double, Double)]): Option[Area] = {

      val outerNodes = outerNodeMapper.outlineNodesFor(r, allRelations, ways, nodes)

      outerNodes.headOption.map { n =>
        val area = new Polygon()
        area.startPath(n._2, n._3)
        outerNodes.drop(1).map { on =>
          area.lineTo(new Point(on._2, on._3))
        }

        Area(render(r), area, boundingBoxFor(area))
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
