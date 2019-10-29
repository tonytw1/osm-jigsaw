package input

import java.io.{File, FileOutputStream}

import com.google.common.io.Files
import play.api.libs.json.Json

trait Boundaries extends WorkingFiles {

  def recordBoundaries(extractName: String, boundaries: Map[String, Long]): Unit = {
    val boundariesFile = new FileOutputStream(boundariesFilepath(extractName))
    boundariesFile.write(Json.toBytes(Json.toJson(boundaries)))
    boundariesFile.close()
  }

  def readBoundaries(extractName: String): Map[String, Long] = {
    val bytes =  Files.toByteArray(new File(boundariesFilepath(extractName)))
    Json.parse(bytes).as[Map[String, Long]]
  }

}