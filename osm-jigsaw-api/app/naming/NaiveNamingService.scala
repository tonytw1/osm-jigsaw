package naming

import javax.inject.Inject
import model.GraphNode
import tags.TagService

class NaiveNamingService @Inject()(tagService: TagService) {

  def nameFor(paths: Seq[Seq[GraphNode]]): String = {
    val pathToUse = paths.head
    pathToUse.map { p =>
      tagService.nameForOsmId(p.area.osmIds.head)
    }.flatten.reverse.mkString(", ")
  }

}
