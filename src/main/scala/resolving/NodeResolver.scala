package resolving

import java.lang

import org.apache.logging.log4j.scala.Logging
import org.mapdb.{HTreeMap, Serializer}

trait NodeResolver {
  def resolvePointForNode(nodeId: Long): Option[(Double, Double)]
}

class InMemoryNodeResolver(nodes: Map[Long, (Double, Double)]) extends NodeResolver {

  def resolvePointForNode(nodeId: Long): Option[(Double, Double)] = {
    nodes.get(nodeId).map(n => (n._1, n._2))
  }

}

import org.mapdb.DBMaker

class MapDBNodeResolver() extends NodeResolver with Logging {

  val map: HTreeMap[lang.Long, Array[Double]] = {
    logger.info("Init'ing node resolver")
    val db = DBMaker.fileDB("nodes.db").fileMmapEnable().make
    val map: HTreeMap[lang.Long, Array[Double]] = db.hashMap("nodes").keySerializer(Serializer.LONG).valueSerializer(Serializer.DOUBLE_ARRAY).create
    logger.info("Done")
    map
  }

  def insert(nodeId: Long, position: (Double, Double)) = {
      map.put(nodeId, Array(position._1, position._2))
  }

  def resolvePointForNode(nodeId: Long): Option[(Double, Double)] = {
    val got: Array[Double] = map.get(nodeId)
    if (got != null) {
      Some(got(0), got(1))
    } else {
      None
    }
  }

}

