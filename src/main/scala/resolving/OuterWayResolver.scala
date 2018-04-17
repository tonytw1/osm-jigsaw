package resolving

import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation}

import scala.collection.JavaConverters._

class OuterWayResolver extends Logging {

  def resolveOuterWayIdsFor(r: Relation, allRelations: Map[Long, Relation], relationSeenSoFar: Set[Long] = Set()): Seq[Long] = {

    val usedRelations = relationSeenSoFar + r.getId

    val outers = r.getMembers.asScala.filterNot{ rm =>
      val isCircularReference = rm.getMemberType == EntityType.Relation && usedRelations.contains(rm.getMemberId)
      if (isCircularReference) {
        logger.warn("Relation " + r.getId + " has suspected circular reference to relation: " + rm.getMemberId)
      }
      isCircularReference

    }.filter(rm => rm.getMemberRole == "outer")
    outers.flatMap { rm =>
      rm.getMemberType match {
        case EntityType.Way => Seq(rm.getMemberId)
        case EntityType.Relation => // TODO want test case for this
          allRelations.get(rm.getMemberId).map { sr =>
            logger.info("Relation " + r.getId + " has subrelation " + rm.getMemberId + " as an outer")
            resolveOuterWayIdsFor(sr, allRelations, usedRelations)
          }.getOrElse {
            logger.warn("Could not resolve outer subrelation " + rm.getMemberId + " for relation " + r + "; ignoring this subrelation")
            Seq()
          }
        case _ => Seq()
      }
    }
  }

}
