package naming

import javax.inject.Inject

import model.OsmId
import tags.TagService

class NaiveNamingService @Inject()(tagService: TagService) {

  def nameFor(paths: Seq[Seq[Seq[OsmId]]]): String = {
    val pathToUse = paths.head
    pathToUse.map { n =>
      tagService.nameForOsmId(n.head)
    }.flatten.reverse.mkString(", ")
  }

}
