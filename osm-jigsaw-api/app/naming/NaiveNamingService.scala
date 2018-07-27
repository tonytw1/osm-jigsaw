package naming

import javax.inject.Inject
import model.OsmId
import tags.TagService

import scala.collection.mutable

class NaiveNamingService @Inject()(tagService: TagService) {

  private val TagsWhichDoNotContributeToLocationNames = Set(
    "boundary" -> "timezone",
    "historic" -> "yes"
  )

  def nameFor(paths: Seq[Seq[Seq[OsmId]]]): String = {

    val pathsWithoutExcludedTags: Seq[Seq[Seq[OsmId]]] = paths.map { path =>
      path.filter { nodeOsmIds =>
        val nodeTags = nodeOsmIds.map { osmId =>
          tagService.tagsFor(osmId)
        }.flatten.flatten.toMap

        nodeTags.toSet.intersect(TagsWhichDoNotContributeToLocationNames).isEmpty
      }
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
      i ++ Seq(a._1, a._2).filter(n => !i.contains(n))
    }

    val names = combined.map { n =>
      tagService.nameForOsmId(n)
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
