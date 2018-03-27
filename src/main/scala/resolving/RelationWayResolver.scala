package resolving

import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation, RelationMember}

import scala.collection.JavaConverters._

class RelationWayResolver {

  def expandRelation(r: Relation, allRelations: Map[Long, Relation]): Seq[Relation] = {
    val relationMembers = r.getMembers.asScala.filter(rm => rm.getMemberType == EntityType.Relation)
    if (relationMembers.isEmpty) {
      Seq(r)

    } else {
      relationMembers.flatMap { rm =>
        println("Recursing to resolve subrelation: " + rm)
        allRelations.get(rm.getMemberId).map { sr =>
          Seq(sr) ++ expandRelation(sr, allRelations)
        }.getOrElse {
          println("Could not find subrelation to resolve: " + rm.getMemberId)
          Seq()
        }
      }
    }
  }

  def resolveOuterWayIdsFor(rs: Seq[Relation], allRelations: Map[Long, Relation]): Seq[Long] = { // TODO want test case for this
    val outers = rs.map(r => r.getMembers.asScala.filter(rm => rm.getMemberRole == "outer")).flatten
    outers.flatMap { rm =>
      rm.getMemberType match {
        case EntityType.Way =>
          Seq(rm.getMemberId)
        case _ => Seq()
      }
    }
  }

}
