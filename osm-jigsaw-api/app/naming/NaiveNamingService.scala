package naming

import javax.inject.Inject
import model.OsmId
import tags.TagService
import com.esri.core.geometry.Point

import scala.collection.mutable

class NaiveNamingService @Inject()(tagService: TagService) {

  private val TagsWhichDoNotContributeToLocationNames = Set(
    "boundary" -> "civil_defense",
    "boundary" -> "diocese",
    "boundary" -> "eez",
    "boundary" -> "fire",
    "boundary" -> "fire_district",
    "boundary" -> "historic",
    "boundary" -> "historic_parish",
    "boundary" -> "maritime",
    "boundary" -> "low_emission_zone",
    "boundary" -> "police",
    "boundary" -> "proposed",
    "boundary" -> "timezone",
    "boundary" -> "vice_county",
    "historic" -> "yes",
    "boundary" -> "historic_administrative",
    "boundary" -> "historic_political",
    "political_division" -> "historical_local_const",
    "political_division" -> "euro_const",
    "political_division" -> "local_const",
    "public_transport" -> "pay_scale_area",
    "type" -> "network",
    "type" -> "toll"
  )

  def nameFor(paths: Seq[Seq[(Seq[OsmId], Double)]], point: Point, requestedLanguage: Option[String] = None): String = {

    def hasExcludedTags(osmId: OsmId): Boolean = {
      val osmIdTags = tagService.tagsFor(osmId, point).getOrElse(Map.empty).toSet
      val excludedTags = osmIdTags.intersect(TagsWhichDoNotContributeToLocationNames)
      excludedTags.nonEmpty
    }

    val pathsWithoutExcludedTags: Seq[Seq[Seq[OsmId]]] = paths.map { path =>
      path.map { p =>
        p._1.filter(e => !hasExcludedTags(e))
      }.filter(_.nonEmpty)
    }

    // Merge the multiple paths into a graph represented with adjacent pairs
    val adjacentPairs: mutable.ListBuffer[(OsmId, OsmId)] = mutable.ListBuffer[(OsmId, OsmId)]()
    pathsWithoutExcludedTags.map { path =>
      path.foldLeft(mutable.Stack[OsmId]()) { (i, a) =>
        a.map { n => // TODO need to decide which name to take for an overlap
          val node = n
          i.headOption.map { l =>
            val pair: (OsmId, OsmId) = ((l, node))
            if (!adjacentPairs.contains(pair)) {
              adjacentPairs += pair
            }
          }
          i.push(node)
        }
        i
      }
    }

    val combined = adjacentPairs.foldLeft(Seq[OsmId]()) { (i, a) =>
      if (!i.contains(a._2)) {
        val insertAfter = i.indexOf(a._1) + 1
        if (insertAfter > 0) {
          val (before, after) = i.splitAt(insertAfter)
          before ++ Seq(a._2) ++ after
        } else {
          i ++ Seq(a._1, a._2)
        }
      } else {
        i
      }
    }

    val areas: Map[OsmId, Double] = paths.flatten.map { i =>
      val a: (Seq[OsmId], Double) = i
      i._1.map { j =>
        (j, i._2)
      }
    }.flatten.toMap

    val sortedByArea = combined.sortBy { o =>
      -areas.get(o).getOrElse(0D)
    }

    val names = sortedByArea.map { n =>
      tagService.nameForOsmId(n, point, requestedLanguage)
    }.flatten


    val withDeduplicatedNames = names.foldLeft(Seq[String]()) { (i, a) =>
      if (!i.contains(a)) {
        i :+ a
      } else {
        i
      }
    }

    withDeduplicatedNames.reverse.mkString(", ")
  }

}
