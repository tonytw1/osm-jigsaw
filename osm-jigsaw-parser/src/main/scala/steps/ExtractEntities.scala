package steps

import java.io.FileOutputStream

import graphing.EntitiesToGraph
import input.{RelationExtractor, WorkingFiles}
import org.apache.logging.log4j.scala.Logging
import play.api.libs.json.Json

class ExtractEntities extends EntitiesToGraph with WorkingFiles with Logging {

  def extract(extractName: String) {

    def recordRecursiveRelations(extractName: String, relationIds: Seq[Long]): Unit = {
      val recursiveRelationsFile = new FileOutputStream(recursiveRelationsFilepath(extractName))
      recursiveRelationsFile.write(Json.toBytes(Json.toJson(relationIds)))
      recursiveRelationsFile.close()
    }

    val outputFilepath = extractedRelsFilepath(extractName)
    logger.info("Extracting entities and their resolved components from " + extractName + " into " + outputFilepath)

    val extractor = new RelationExtractor()
    extractor.extract(extractName, entitiesToGraph, outputFilepath)

    logger.info("Dumping discovered recursive relations")
    recordRecursiveRelations(extractName, extractor.recursiveRelations())

    logger.info("Done")
  }

}
