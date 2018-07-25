package naming

import javax.inject.Inject

import model.OsmId
import tags.TagService

class NaiveNamingService @Inject()(tagService: TagService) {

  private val ExcludedTags = Set(("boundary", "timezone"))

  def nameFor(paths: Seq[Seq[Seq[OsmId]]]): String = {
    val pathToUse: Seq[Seq[OsmId]] = paths.head  // TODO naive and ignores interesting nodes the other paths

    val withoutExcludedTags: Seq[Seq[OsmId]] = pathToUse.filter { nodeOsmIds =>
      val nodeTags: Map[String, String] = nodeOsmIds.map { osmId =>
        tagService.tagsFor(osmId)
      }.flatten.flatten.toMap

      nodeTags.toSet.intersect(ExcludedTags).isEmpty
    }

    val names = withoutExcludedTags.map { n =>
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
