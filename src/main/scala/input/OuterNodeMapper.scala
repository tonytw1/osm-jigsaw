package input

import org.openstreetmap.osmosis.core.domain.v0_6._

import scala.collection.JavaConverters._
import scala.collection.mutable

class OuterNodeMapper(ways: Map[Long, Way], nodes: Map[Long, Node]) {

  def outerNodesFor(r: Relation): Seq[Node] = { // TODO handle missing Ways and nodes

    try {
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

      val outerWayRelationMembers = nonNodes.filter(rm => rm.getMemberType == EntityType.Way) // TODO resolve sub relations

      val outerWays: Seq[Option[Way]] = resolveWaysFor(outerWayRelationMembers)

      val wayGroups: Seq[Seq[Long]] = outerWays.flatten.map { w =>
        val nodes: Seq[Long] = w.getWayNodes.asScala.map(wn => wn.getNodeId)
        nodes
      }.filter(wg => wg.nonEmpty)

      val joinedNodeIds = {

        val meh: mutable.Set[Seq[Long]] = mutable.Set() ++ wayGroups

        val first: Seq[Long] = meh.head
        var joined: Seq[Seq[Long]] = Seq(first)
        meh.remove(first)

        val q = meh.exists(wg => wg.head == joined.last.last || wg.last == joined.last.last)

        while (meh.nonEmpty && meh.exists(wg => wg.head == joined.last.last || wg.last == joined.last.last)) {
          val next = meh.find(wg => wg.head == joined.last.last  || wg.last == joined.last.last).get
          if (next.head == joined.last.last) {
            joined = joined :+ next
          } else {
            joined = joined :+ next.reverse
          }
          meh.remove(next)
        }
        joined

      }.flatten

      joinedNodeIds.map { nid =>
        nodes.get(nid)
      }.flatten

    } catch {
      case e: Exception =>
        println("Failed to compose relation: " + r.getId, e.getMessage)
        Seq()
    }
  }

}
