package input

import org.openstreetmap.osmosis.core.domain.v0_6._

import scala.collection.JavaConverters._

class OuterNodeMapper(ways: Map[Long, Way], nodes: Map[Long, Node]) {

  def outerNodesFor(r: Relation): Seq[Node] = {  // TODO handle missing Ways and nodes

    def resolveWaysFor(wayRelationMembers: Seq[RelationMember]): Seq[Option[Way]] = {
      wayRelationMembers.map { rm =>
        ways.get(rm.getMemberId)
      }
    }

    val relationMembers = r.getMembers.asScala
    val nonNodes = relationMembers.filter(rm => rm.getMemberType != EntityType.Node)
    val outers = nonNodes.filter(rm => rm.getMemberRole == "outer")

    outers.filter(rm => rm.getMemberType == EntityType.Relation).map { r =>
      println("Need to resolve sub relation in outer role: " + r)
    }

    val outerWayRelationMembers = nonNodes.filter(rm => rm.getMemberType == EntityType.Way)  // TODO resolve sub relations

    val outerWays = resolveWaysFor(outerWayRelationMembers)
    val outerNodesIds = outerWays.flatten.map { w =>
      w.getWayNodes.asScala.map(wn => wn.getNodeId)
    }.flatten

    outerNodesIds.map { nid =>
      nodes.get(nid)
    }.flatten
  }

}
