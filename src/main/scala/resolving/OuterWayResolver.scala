package resolving

import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation}

import scala.collection.JavaConverters._

class OuterWayResolver {

  def resolveOuterWayIdsFor(r: Relation, allRelations: Map[Long, Relation]): Seq[Long] = { // TODO want test case for this
    val outers = r.getMembers.asScala.filter(rm => rm.getMemberRole == "outer")
    outers.flatMap { rm =>
      rm.getMemberType match {
        case EntityType.Way => Seq(rm.getMemberId)
        case EntityType.Relation  => Seq()  // TODO recurse when an example is found
        case _ => Seq()
      }
    }
  }

}
