import input.{RelationExtractor, TestValues}
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, EntityType}
import org.scalatest.FlatSpec
import resolving.{LoadTestEntities, RelationResolver}

class ExtractTestRelationsSpec extends FlatSpec with TestValues with LoadTestEntities {

  "extract test relations" should "build pbf file containing test relations" in {
    val relationsToExtract = Seq(LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION, BOURNEMOUTH)

    def predicate(entity: Entity): Boolean = {
      relationsToExtract.exists(i => entity.getId == i._1 && entity.getType == i._2)
    }

    val relationExtractor: RelationExtractor = new RelationExtractor()
    val extractedRelationsWithComponents = relationExtractor.extract("great-britain-latest.osm.pbf", predicate, "gb-test-data.pbf")

    succeed
  }

  def loadTestEntity(target: (Long, EntityType), file: String): Option[Entity] = {
    def predicate(entity: Entity): Boolean = entity.getId == target._1 && entity.getType == target._2
    loadEntities(file, predicate).headOption
  }

}
