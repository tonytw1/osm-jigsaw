package input

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import play.api.libs.json.Json

trait Boundaries extends WorkingFiles {

  def recordBoundaries(boundaries: Map[String, Long]): Unit = {
    val boundariesFile = new FileOutputStream(boundariesFilepath)
    boundariesFile.write(Json.toBytes(Json.toJson(boundaries)))
    boundariesFile.close()
  }

  def readBoundaries(): Map[String, Long] = {
    val bytes =  Files.toByteArray(new File(boundariesFilepath))
    Json.parse(bytes).as[Map[String, Long]]
  }

  def recordRecursiveRelations(relationIds: Seq[Long]): Unit = {
    val recursiveRelationsFile = new FileOutputStream(recursiveRelationsFilepath)
    recursiveRelationsFile.write(Json.toBytes(Json.toJson(relationIds)))
    recursiveRelationsFile.close()
  }

}