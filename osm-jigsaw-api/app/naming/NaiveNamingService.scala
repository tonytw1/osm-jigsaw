package naming

import javax.inject.Inject

import model.OsmId
import tags.TagService

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

    val combined = pathsWithoutExcludedTags.flatten // TODO needs a more sensible algorithm

    val names = combined.map { n =>
      tagService.nameForOsmId(n.head)
    }.flatten

    val withoutConsecutiveDuplicateNames = names.foldLeft(Seq[Option[String]]()) { (i, a) =>
      val previous = i.lastOption.flatten
      val toAdd = if (Some(a) != previous) {
        Some(a)
      } else {
        None
      }
      i :+ toAdd
    }.flatten

    withoutConsecutiveDuplicateNames.reverse.mkString(", ")
  }

}
