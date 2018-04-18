package resolving

import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation}

import scala.collection.JavaConverters._

class RelationExpander extends Logging {

  def expandRelation(r: Relation, allRelations: Map[Long, Relation], parent: Option[Relation] = None): Seq[Relation] = {
    val relationMembers = r.getMembers.asScala.filter(rm => rm.getMemberType == EntityType.Relation)
    if (relationMembers.isEmpty) {
      Seq(r)

    } else {
      logger.debug("Relation " + r.getId + " has " + relationMembers.size + " relation members which are relations.")
      if (relationMembers.exists(rm => {
        val maybeLong: Option[Long] = parent.map(p => p.getId)
        Some(rm.getMemberId) == maybeLong
      })) { // TODO won't quick circles which are more than two nodes
        logger.warn("Relation " + r.getId + " has a relation member which references it as a relation member; ignoring potential infinite loop: " + r.getId)
        Seq()

      } else {
        relationMembers.flatMap { rm =>
          logger.debug("Recursing to resolve subrelation: " + rm)
          allRelations.get(rm.getMemberId).map { sr =>
            Seq(r) ++ expandRelation(sr, allRelations, Some(r))
          }.getOrElse {
            logger.warn("Could not find subrelation to resolve: " + rm.getMemberId)
            Seq()
          }
        }
      }
    }
  }

}
