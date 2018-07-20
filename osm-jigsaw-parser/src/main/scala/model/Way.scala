package model

case class Way(id: Long, nodes: Seq[Long])
case class JoinedWay(way: model.Way, reverse: Boolean)
