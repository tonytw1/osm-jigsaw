package tags

import org.specs2.mutable._

class EntityNameTagsSpec extends Specification with EntityNameTags {

  "use the preferred language if available" in {
    val tags = Map(
      "name" -> "Deutschland",
      "name:en" -> "Federal Republic of Germany")

    val name = getNameFromTags(tags, "en").get

    name must equalTo("Federal Republic of Germany")
  }

  "fall back to default name if preferred language is not available" in {
    val tags = Map(
      "name:ace" -> "Jeureuman",
      "name" -> "Deutschland",
      "name:gb" -> "Alemaña")

    val name = getNameFromTags(tags, "en").get

    name must equalTo("Deutschland")
  }

  "Use the shortest available preferred language response if multiple options are given" in {
    val tags = Map(
      "name" -> "Deutschland",
      "name:en" -> "Federal Republic of Germany",
      "name:en" -> "Germany")

    val name = getNameFromTags(tags, "en").get

    name must equalTo("Germany")
  }

  "Other available names are considered in stated order" in {
    // Brownsea Island has an odd address:housename special case;
    // based on this example name is the best fall back if name:en is not available
    val tags = Map(
      "addr:city" -> "Poole",
      "addr:housename" -> "Poole Harbour",
      "name" -> "Brownsea Island",
      "name:it" -> "Isola di Brownsea"
    )

    val name = getNameFromTags(tags, "en").get

    name must equalTo("Brownsea Island")
  }

}
