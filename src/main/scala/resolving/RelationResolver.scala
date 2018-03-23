package resolving

class RelationResolver {

  // Given a relation, turn the way ids forming it's outer.
  // If the outer contains sub relation, recursively resolve them.
  // TODO return in order of appearance even through the source data is not always in a continuous order
  def resolveOuterWayIdsFor(r: Relation): Set[Long] = {
    val members = r.getMembers.asScala
    val outers = members.filter(rm => rm.getMemberRole == "outer")
    val y = outers.map { rm =>
      val z = rm.getMemberType match {
        case EntityType.Way =>
          Seq(rm.getMemberId)
        case EntityType.Relation =>
          val subRelation = allRelations.get(rm.getMemberId)
          println("Relation " + r + " has sub relation " + rm.getMemberId + ": " + rel)
          rel.map { r2 =>
            val ws = resolveOuterWaysIds(r2)
            println("Resolved relation " + r2 + " to ways: " + ws)
            ws
          }.getOrElse {
            println("Subrelation not found; ignoring: " + rm.getMemberId)
            Seq()
          }
        case _ =>
          Seq()
      }
      z.toSet
    }.flatten
    y.toSet
  }

}
