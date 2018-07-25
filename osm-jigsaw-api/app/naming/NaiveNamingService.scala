package naming

import javax.inject.Inject

import model.OsmId
import tags.TagService

class NaiveNamingService @Inject()(tagService: TagService) {

  def nameFor(paths: Seq[Seq[Seq[OsmId]]]): String = {
    val pathToUse = paths.head  // TODO naive and ignores interesting nodes the other paths

    val names = pathToUse.map { n =>
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
