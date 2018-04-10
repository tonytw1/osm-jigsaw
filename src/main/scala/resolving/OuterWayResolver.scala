package resolving

import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation}

import scala.collection.JavaConverters._

class OuterWayResolver extends Logging {

  def resolveOuterWayIdsFor(r: Relation, allRelations: Map[Long, Relation]): Seq[Long] = {
    val outers = r.getMembers.asScala.filterNot(rm => rm.getMemberType == EntityType.Relation && rm.getMemberId == r.getId).filter(rm => rm.getMemberRole == "outer")
    outers.flatMap { rm =>
      rm.getMemberType match {
        case EntityType.Way => Seq(rm.getMemberId)
        case EntityType.Relation => // TODO want test case for this
          allRelations.get(rm.getMemberId).map { sr =>
            logger.info("Relation " + r.getId + " has subrelation as an outer")
            resolveOuterWayIdsFor(sr, allRelations)
          }.getOrElse {
            logger.warn("Could not resolve outer subrelation " + rm.getMemberId + " for relation " + r + "; ignoring this subrelation")
            Seq()
          }
        case _ => Seq()
      }
    }
  }

}
