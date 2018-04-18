package resolving

import java.lang

import org.apache.logging.log4j.scala.Logging
import org.mapdb.{HTreeMap, Serializer, SortedTableMap}

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

  val map = {
    logger.info("Init'ing node resolver")

    import org.mapdb.volume.MappedFileVol
    val volume = MappedFileVol.FACTORY.makeVolume("nodes.vol", true)

    val map = SortedTableMap.open(
        volume,
        Serializer.LONG,
        Serializer.DOUBLE_ARRAY
      )

    logger.info("Done")
    map
  }

  def insert(nodeId: Long, position: (Double, Double)) = {
    //map.put(nodeId, Array(position._1, position._2))
  }

  def resolvePointForNode(nodeId: Long): Option[(Double, Double)] = {
    val got: Array[Double] = map.get(nodeId)
    if (got != null) {
      Some(got(0), got(1))
    } else {
      logger.warn("Could not resolve node: " + nodeId)
      None
    }
  }

  def close(): Unit = {
    map.close()
  }

}

