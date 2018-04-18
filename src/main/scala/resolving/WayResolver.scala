package resolving

import model.Way
import org.apache.logging.log4j.scala.Logging
import org.mapdb.{Serializer, SortedTableMap}

trait WayResolver {
  def get(wayId: Long): Option[Way]
}

class InMemoryWayResolver(ways: Map[Long, Way]) extends WayResolver {
  def get(wayId: Long): Option[Way] = {
    ways.get(wayId)
  }
}

class MapDBWayResolver() extends WayResolver with Logging {

  val map = {
    logger.info("Init'ing node resolver")

    import org.mapdb.volume.MappedFileVol
    val volume = MappedFileVol.FACTORY.makeVolume("ways.vol", true)

    val map = SortedTableMap.open(
      volume,
      Serializer.LONG,
      Serializer.LONG_ARRAY
    )

    logger.info("Done")
    map
  }

  def get(wayId: Long): Option[Way] = {
    val got: Array[Long] = map.get(wayId)
    if (got != null) {
      Some(Way(id = wayId, nodes = got.toSeq))
    } else {
      logger.warn("Could not resolve way: " + wayId)
      None
    }
  }

  def close(): Unit = {
    map.close()
  }

}