package resolving

import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation}

import scala.collection.JavaConverters._

class OuterWayResolver {

  def resolveOuterWayIdsFor(r: Relation, allRelations: Map[Long, Relation]): Seq[Long] = {
    val outers = r.getMembers.asScala.filter(rm => rm.getMemberRole == "outer")
    outers.flatMap { rm =>
      rm.getMemberType match {
        case EntityType.Way => Seq(rm.getMemberId)
        case EntityType.Relation => // TODO want test case for this
          allRelations.get(rm.getMemberId).map { sr =>
            println("Relation " + r + " has subrelation as an outer")
            resolveOuterWayIdsFor(sr, allRelations)
          }.getOrElse {
            println("Could not resolve outer subrelation " + rm.getMemberId + " for relation " + r)
            Seq()
          }
        case _ => Seq()
      }
    }
  }

}
