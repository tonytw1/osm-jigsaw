package resolving

import org.openstreetmap.osmosis.core.domain.v0_6.{EntityType, Relation}

import scala.collection.JavaConverters._

class RelationWayResolver {

  def resolveOuterWayIdsFor(r: Relation, allRelations: Map[Long, Relation]): Seq[Long] = { // TODO want test case for this
    val outers = r.getMembers.asScala.filter(rm => rm.getMemberRole == "outer")

    outers.flatMap { rm =>
      rm.getMemberType match {
        case EntityType.Way =>
          Seq(rm.getMemberId)
        case EntityType.Relation => {
          println("Recursing to resolve outer subrelation: " + rm)
          allRelations.get(rm.getMemberId).map(sr => resolveOuterWayIdsFor(sr, allRelations)).getOrElse {
            println("Could not find subrelation to resolve: " + rm.getMemberId)
            Seq()
          }
        }
        case _ => Seq()
      }
    }
  }

}
