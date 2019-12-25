package steps

import java.io.{BufferedOutputStream, FileInputStream, FileOutputStream}

import input.{SinkRunner, WorkingFiles}
import org.apache.logging.log4j.scala.Logging
import org.openstreetmap.osmosis.core.domain.v0_6.{Entity, Relation, Way}
import outputresolvedarea.OutputResolvedArea
import outputway.OutputWay
import progress.{CommaFormattedNumbers, ProgressCounter}
import resolving.{AreaResolver, MapDBNodeResolver, MapDBWayResolver, ResolvedArea}

import scala.collection.immutable.LongMap
import scala.collection.mutable

class ExtractAreas extends WorkingFiles with EntitiesToGraph with CommaFormattedNumbers with Logging {

  def resolveAreaWays(extractName: String): Unit = {

    def all(entity: Entity): Boolean = true

    var allRelations = LongMap[Relation]()
    var closedWaysToResolve = Set[Way]()

    val entityLoadProgress = new ProgressCounter(step = 100000)

    def loadIntoMemory(entity: Entity) = {
      entityLoadProgress.withProgress {
        entity match {
          case r: Relation => allRelations = allRelations + (r.getId -> r)
          case w: Way => closedWaysToResolve = closedWaysToResolve + w
          case _ =>
        }
      }
    }

    // Load all relations into an in memory map
    val relsInputFilepath = extractedRelsFilepath(extractName)
    logger.info("Loading entities from: " + relsInputFilepath)
    new SinkRunner(new FileInputStream(relsInputFilepath), all, loadIntoMemory).run
    logger.info("Finished loading entities")

    val areawaysFilepath = areaWaysFilepath(extractName)
    logger.info("Resolving relation areas into: " + areawaysFilepath)
    val resolvedAreasOutput = new BufferedOutputStream(new FileOutputStream(areawaysFilepath))

    var counter = 0
    val waysUsed = mutable.Set[model.Way]()

    def outputAreasToFileAndCacheUsedWays(resolvedAreas: Seq[ResolvedArea]): Unit = {
      resolvedAreas.foreach { ra =>
        counter = counter + 1
        waysUsed ++= ra.outline.map(_.way)
        val signedWays = ra.outline.map(w => w.way.id * (if (w.reverse) -1 else 1))
        OutputResolvedArea(id = Some(ra.id), osmId = Some(ra.osmId), ways = signedWays).writeDelimitedTo(resolvedAreasOutput)
      }
    }

    // For each top level relation resolve it into a collection of area outlines made up of ways
    logger.info("Filtering relations to resolve")
    val relationsToResolve = allRelations.values.filter(e => entitiesToGraph(e))
    logger.info("Resolving areas for " + relationsToResolve.size + " relations")

    val areaResolver = new AreaResolver()
    val wayResolver = new MapDBWayResolver(relsInputFilepath + ".ways.vol")

    areaResolver.resolveAreas(relationsToResolve, allRelations, wayResolver, outputAreasToFileAndCacheUsedWays)

    // For each closed way resolve it into an Area outline
    logger.info("Resolving areas for " + commaFormatted(closedWaysToResolve.size) + " ways")
    areaResolver.resolveAreas(closedWaysToResolve, allRelations, wayResolver, outputAreasToFileAndCacheUsedWays) // TODO why are two sets of ways in scope?
    wayResolver.close()

    resolvedAreasOutput.flush()
    resolvedAreasOutput.close()

    logger.info("Dumped " + commaFormatted(counter) + " areas to file: " + areawaysFilepath)
    logger.info("Collected " + commaFormatted(waysUsed.size) + " ways in the process")

    // For every way used in area outlines extract the node points which make up tha outline way
    logger.info("Resolving points for used ways")
    val nodeResolver = new MapDBNodeResolver(relsInputFilepath + ".nodes.vol")

    val areaWaysOutput = new BufferedOutputStream(new FileOutputStream(areaWaysWaysFilePath(extractName)))

    val wayCounter = new ProgressCounter(10000, total = Some(waysUsed.size))
    waysUsed.foreach { w =>
      wayCounter.withProgress {
        val points = w.nodes.flatMap { nid =>
          nodeResolver.resolvePointForNode(nid) // TODO handle missing node
        }
        val outputWay = OutputWay(id = Some(w.id), latitudes = points.map(i => i._1), longitudes = points.map(i => i._2))
        outputWay.writeDelimitedTo(areaWaysOutput)
      }
    }
    areaWaysOutput.flush()
    areaWaysOutput.close()
    logger.info("Done")
  }

}
