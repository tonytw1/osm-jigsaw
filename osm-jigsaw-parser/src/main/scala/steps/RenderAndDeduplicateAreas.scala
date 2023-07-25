package steps

import areas.AreaComparison
import input.{AreaReading, Extracts}
import model.{Area, AreaIdSequence}
import org.apache.logging.log4j.scala.Logging
import output.{AreaWriting, OutputFiles}
import outputresolvedarea.OutputResolvedArea
import outputway.OutputWay
import progress.ProgressCounter
import resolving.PolygonBuilding

import java.io.{BufferedOutputStream, FileOutputStream, InputStream}
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RenderAndDeduplicateAreas extends Extracts with AreaComparison with PolygonBuilding
  with AreaReading with AreaWriting with OutputFiles with Logging {

  def resolveAreas(extractName: String): Unit = {
    val areawaysInputFile = areaWaysFilepath(extractName)
    val areasFilepath = areasFilePath(extractName)

    def buildAreas: Unit = {
      val areawaysWaysFilepath = areaWaysWaysFilePath(extractName)

      logger.info("Reading area ways from file: " + areawaysWaysFilepath)
      val ways = mutable.Map[Long, OutputWay]() // TODO just the points

      def readWay(inputStream: InputStream): scala.Option[OutputWay] = OutputWay.parseDelimitedFrom(inputStream)

      def cacheWay(outputWay: OutputWay) = ways.put(outputWay.id.get, outputWay)

      processPbfFile(areawaysWaysFilepath, readWay, cacheWay)

      val counter = new ProgressCounter(1000)
      val areasOutput = new BufferedOutputStream(new FileOutputStream(areasFilepath))

      def populateAreaNodesAndExportAreasToFile(ra: OutputResolvedArea): Unit = {
        counter.withProgress {
          val outline = ra.ways.flatMap { signedWayId =>
            val l = Math.abs(signedWayId)
            val joined = ways.get(l).map { way =>
              val points = way.latitudes.zip(way.longitudes)
              if (signedWayId < 0) points.reverse else points
            }
            if (joined.isEmpty) {
              logger.warn("Failed to resolve way id: " + l)
            }
            joined
          }

          val outerPoints: Seq[(Double, Double)] = outline.flatten
          polygonForPoints(outerPoints).map { p =>
            exportArea(Area(AreaIdSequence.nextId, p, boundingBoxFor(p), ListBuffer(ra.osmId.get), areaOf(p)), areasOutput)
          }
        }
      } // TODO isolate for reuse in test fixtures

      logger.info("Resolving areas")

      def readResolvedArea(inputStream: InputStream) = OutputResolvedArea.parseDelimitedFrom(inputStream)

      logger.info("Expanding way areas")
      processPbfFile(areawaysInputFile, readResolvedArea, populateAreaNodesAndExportAreasToFile)

      areasOutput.flush()
      areasOutput.close()
      logger.info("Dumped areas to file: " + areasFilepath)
    }

    def deduplicate = {
      logger.info("Deduplicating areas")

      def deduplicateAreas(areas: Seq[Area]): Seq[Area] = {
        logger.info("Sorting areas by size")
        val sortedAreas = areas.sortBy(_.area)

        val deduplicatedAreas = mutable.ListBuffer[Area]()

        val deduplicationCounter = new ProgressCounter(1000, Some(areas.size))
        sortedAreas.foreach { a =>
          deduplicationCounter.withProgress {
            var ok = deduplicatedAreas.nonEmpty
            val i = deduplicatedAreas.iterator
            var found: scala.Option[Area] = None
            while (ok) {
              val x = i.next()
              if (x.area == a.area && areaSame(x, a)) {
                found = Some(x)
              }
              ok = x.area >= a.area && i.hasNext
            }

            found.map { e =>
              e.osmIds ++= a.osmIds
            }.getOrElse {
              deduplicatedAreas.+=:(a)
            }
          }
        }
        deduplicatedAreas
      }

      val areas = readAreasFromPbfFile(areasFilepath)
      val deduplicatedAreas = deduplicateAreas(areas)
      logger.info("Deduplicated " + areas.size + " areas to " + deduplicatedAreas.size)

      logger.info("Writing deduplicated areas to file")
      val finalOutput = new BufferedOutputStream(new FileOutputStream(areasFilepath))
      val outputCounter = new ProgressCounter(100000, Some(deduplicatedAreas.size))
      deduplicatedAreas.foreach { a =>
        outputCounter.withProgress {
          exportArea(a, finalOutput)
        }
      }
      finalOutput.flush()
      finalOutput.close()
      logger.info("Wrote deduplicated areas to file: " + areasFilepath)
    }

    buildAreas
    deduplicate
  }

}
