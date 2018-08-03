package naming

import javax.inject.Inject
import model.OsmId
import tags.TagService

import scala.collection.mutable

class NaiveNamingService @Inject()(tagService: TagService) {

  private val TagsWhichDoNotContributeToLocationNames = Set(
    "boundary" -> "eez",
    "boundary" -> "fire",
    "boundary" -> "historic",
    "boundary" -> "maritime",
    "boundary" -> "police",
    "boundary" -> "proposed",
    "boundary" -> "timezone",
    "boundary" -> "vice_county",
    "historic" -> "yes",
    "political_division" -> "euro_const",
    "political_division" -> "local_const",
    "type" -> "toll"
  )

  def nameFor(paths: Seq[Seq[Seq[OsmId]]], requestedLanguage: Option[String] = None): String = {

    def hasExcludedTags(osmId: OsmId): Boolean = {
      val osmIdTags = tagService.tagsFor(osmId).getOrElse(Map.empty).toSet
      val excludedTags = osmIdTags.intersect(TagsWhichDoNotContributeToLocationNames)
      excludedTags.nonEmpty
    }

    val pathsWithoutExcludedTags: Seq[Seq[Seq[OsmId]]] = paths.map { path =>
      path.map { p =>
        p.filter(e => !hasExcludedTags(e))
      }.filter(_.nonEmpty)
    }

    // Merge the multiple paths into a graph represented with adjacent pairs
    val adjacentPairs: mutable.ListBuffer[(OsmId, OsmId)] = mutable.ListBuffer[(OsmId, OsmId)]()
    pathsWithoutExcludedTags.map { path =>
      path.foldLeft(mutable.Stack[OsmId]()) { (i, a) =>
        a.map { n =>  // TODO need to decide which name to take for an overlap
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

    val names = combined.map { n =>
      tagService.nameForOsmId(n, requestedLanguage)
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
