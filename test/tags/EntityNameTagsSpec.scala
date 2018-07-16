package tags

import org.specs2.mutable._

class EntityNameTagsSpec extends Specification with EntityNameTags {

  "use the preferred language if available" in {
    val tags = Seq(("name", "Deutschland"), ("name:en", "Federal Republic of Germany"))

    val name = getNameFromTags(tags).get

    name must equalTo("Federal Republic of Germany")
  }

}
