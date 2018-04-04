package model

case class Way(id: Long, osmId: String, name: String, nodes: Seq[Long])
case class JoinedWay(way: model.Way, reverse: Boolean)
