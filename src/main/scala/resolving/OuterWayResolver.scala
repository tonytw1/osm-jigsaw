package resolving

import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation}

import scala.collection.JavaConverters._

class OuterWayResolver {

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
