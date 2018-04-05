package model

case class Way(id: Long, name: Option[String], nodes: Seq[Long])
case class JoinedWay(way: model.Way, reverse: Boolean)
