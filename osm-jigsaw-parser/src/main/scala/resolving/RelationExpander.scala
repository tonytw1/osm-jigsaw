package resolving

import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation}

import scala.collection.JavaConverters._
import scala.collection.mutable

class RelationExpander extends Logging {

  val recursingRelations = mutable.ListBuffer[Long]()

  def expandRelation(r: Relation, allRelations: Map[Long, Relation], upstream: Set[Relation] = Set()): Option[Seq[Relation]] = {
    val relationMembers = r.getMembers.asScala.filter(rm => rm.getMemberType == EntityType.Relation)
    if (relationMembers.exists(rm => upstream.exists(u => u.getId == rm.getMemberId))) {
      logger.warn("Relation " + r.getId + " has a relation member which references it as a relation member; ignoring potential infinite loop")
      recursingRelations += r.getId
      None

    } else {
      val expandedRelationMembers = relationMembers.map { rm =>
        val subrelation = allRelations.get(rm.getMemberId)
        if (subrelation.isEmpty) {
          logger.warn("Could not find sub relation " + rm.getMemberId + " of relation " + r.getId)
        }
        subrelation.flatMap { sr =>
          expandRelation(sr, allRelations, upstream + r)
        }
      }

      if (expandedRelationMembers.exists(e => e.isEmpty)) {
        logger.warn("Failed to expand relation " + r.getId + " due to missing sub relation")
        None

      } else {
        Some(Seq(r) ++ expandedRelationMembers.flatten.flatten)
      }
    }
  }

}
