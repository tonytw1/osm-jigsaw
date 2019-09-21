import java.io.{File, FileInputStream}

import com.google.common.io.Files
import input.SinkRunner
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec

class SinksSpec extends FlatSpec {

  "sinks" should "replicate split entity files using slices of the extract" in {

    val sourceFile = getClass.getClassLoader.getResource("andorra-190917.osm.pbf").getFile

    // Open extract and count the entities
    var nodes = 0L
    var ways = 0L
    var relations = 0L

    def all(entity: Entity): Boolean = true

    def countTypes(entity: Entity) = {
      entity match {
        case n: Node => nodes = nodes + 1
        case w: Way => ways = ways + 1
        case r: Relation => relations = relations + 1
        case _ =>
      }
    }
    new SinkRunner(new FileInputStream(sourceFile), all, countTypes).run

    // Obtain the entity type offsets
    var sink: SinkRunner = null
    var currentType: scala.Option[EntityType] = None
    var currentPosition = 0L

    var boundaries: Seq[(EntityType, Long)] = Seq.empty
    def scanForBoundaries(entity: Entity) = {
      val entityType = scala.Option(entity.getType)
      if (entityType != currentType) {
        boundaries = boundaries :+ (entity.getType, currentPosition)
        currentType = entityType
      }
      currentPosition = sink.currentPosition
    }

    sink = new SinkRunner(new FileInputStream(sourceFile), all, scanForBoundaries)
    sink.run

    val startOfNodes = boundaries.find(_._1 == EntityType.Node).get._2
    val startOfWays = boundaries.find(_._1 == EntityType.Way).get._2
    val startOfRelations = boundaries.find(_._1 == EntityType.Relation).get._2
    val eof = sink.currentPosition

    // Open truncated inputs file and try to read the same content
    val expectedRelations = relations
    val expectedNodes = nodes
    val expectedWays = ways

    nodes = 0
    ways = 0
    relations = 0
    // Deliberate out of order reads
    new SinkRunner(Files.asByteSource(new File(sourceFile)).slice(startOfRelations, eof - startOfRelations).openStream(), all, countTypes).run
    new SinkRunner(Files.asByteSource(new File(sourceFile)).slice(startOfWays, startOfRelations - startOfWays).openStream(), all, countTypes).run
    new SinkRunner(Files.asByteSource(new File(sourceFile)).slice(startOfNodes, startOfWays - startOfNodes).openStream(), all, countTypes).run

    assert(nodes == expectedNodes)
    assert(ways == expectedWays)
    assert(relations == expectedRelations)
  }

}
