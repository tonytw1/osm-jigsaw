package steps

import java.io.InputStream

import model.Area
import org.apache.logging.log4j.scala.Logging
import outputarea.OutputArea
import resolving.{BoundingBox, PolygonBuilding}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait AreaReading extends ProtocolbufferReading with PolygonBuilding with BoundingBox with Logging {

  def readAreasFromPbfFile(inputFilename: String): Seq[Area] = {
    logger.info("Reading areas")
    var areas = ListBuffer[Area]()
    var withOsm = 0

    def loadArea(outputArea: OutputArea) = {
      outputAreaToArea(outputArea).fold {
        logger.warn("Could not build areas from: " + outputArea)
      } { a =>
        if (a.osmIds.nonEmpty) {
          withOsm = withOsm + 1
        }
        areas = areas += a
      }
    }

    processPbfFile(inputFilename, readArea, loadArea)

    logger.info("Read " + areas.size + " areas")
    logger.info("Of which " + withOsm + " had OSM ids")
    areas.toList
  }

  def readAreaOsmIdsFromPbfFile(inputFilename: String): Set[String] = {
    val seenOsmIds = mutable.Set[String]()

    def captureOsmId(outputArea: OutputArea) = seenOsmIds ++= outputArea.osmIds

    processPbfFile(inputFilename, readArea, captureOsmId)

    seenOsmIds.toSet
  }

  private def readArea(inputStream: InputStream): scala.Option[OutputArea] = {
    OutputArea.parseDelimitedFrom(inputStream)
  }

  private def outputAreaToArea(oa: OutputArea): scala.Option[Area] = {
    val points: Seq[(Double, Double)] = (oa.latitudes zip oa.longitudes).map(ll => (ll._1, ll._2))
    polygonForPoints(points).map { p =>
      Area(id = oa.id.get, polygon = p, boundingBox = boundingBoxFor(p), osmIds = ListBuffer() ++ oa.osmIds, oa.area.get) // TODO Naked gets outline
    }
  }

}
