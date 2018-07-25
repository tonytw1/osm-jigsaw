package tags

import org.specs2.mutable._

class EntityNameTagsSpec extends Specification with EntityNameTags {

  "use the preferred language if available" in {
    val tags = Seq(("name", "Deutschland"), ("name:en", "Federal Republic of Germany")).toMap

    val name = getNameFromTags(tags).get

    name must equalTo("Federal Republic of Germany")
  }

  "fall back to default name if preferred langauge is not available" in {
    val tags = Seq(("name:ace", "Jeureuman"), ("name", "Deutschland"), ("name:gb", "Alemaña")).toMap

    val name = getNameFromTags(tags).get

    name must equalTo("Deutschland")
  }

  "Use the shortest available preferred language responnse if multiple options are given" in {
    val tags = Seq(("name", "Deutschland"), ("name:en", "Federal Republic of Germany"), ("name:en", "Germany")).toMap

    val name = getNameFromTags(tags).get

    name must equalTo("Germany")
  }

}
