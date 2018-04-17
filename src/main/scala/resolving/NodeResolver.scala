package resolving

import java.io.File
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
    val db = DBMaker.fileDB(new File("nodes.db")).fileMmapEnable().asyncWriteEnable().make
    val map = db.hashMap("nodes", Serializer.LONG, Serializer.DOUBLE_ARRAY)
    logger.info("Done")
    map
  }

  def insert(nodeId: Long, position: (Double, Double)) = {
      map.put(nodeId, Array(position._1, position._2))
  }

  def resolvePointForNode(nodeId: Long): Option[(Double, Double)] = {
    val got = map.get(nodeId)
    if (got != null) {
      Some(got(0), got(1))
    } else {
      None
    }
  }

}

