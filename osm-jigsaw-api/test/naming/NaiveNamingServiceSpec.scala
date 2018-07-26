package naming

import model.OsmId
import org.mockito.Matchers.any
import org.mockito.Mockito
import org.specs2.mutable._
import tags.TagService

class NaiveNamingServiceSpec extends Specification {

  private val R = "R".charAt(0)

  "place name is a concatenation of the the enclosing area names" in {
    val australia = OsmId(80500L, R)
    val westernAustralia = OsmId(2316598, R)
    val ngaanyatjarra = OsmId(8165171, R)

    val paths = Seq(Seq(Seq(australia), Seq(westernAustralia), Seq(ngaanyatjarra)))
    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])

    Mockito.when(tagServiceMock.nameForOsmId(australia)).thenReturn(Some("Australia"))
    Mockito.when(tagServiceMock.nameForOsmId(westernAustralia)).thenReturn(Some("Western Australia"))
    Mockito.when(tagServiceMock.nameForOsmId(ngaanyatjarra)).thenReturn(Some("Ngaanyatjarra Indigenous Protected Area"))
    Mockito.when(tagServiceMock.tagsFor(any[OsmId])).thenReturn(None)

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths)

    name must equalTo("Ngaanyatjarra Indigenous Protected Area, Western Australia, Australia")
  }

  "consecutive duplicated place names are not adding value and can be removed" in {
    val isleOfManAdminBoundary = OsmId(62269, R)
    val isleOfManIsland = OsmId(6041206, R)
    val middle = OsmId(1061146, R)
    val douglas = OsmId(1061138, R)

    val paths = Seq(Seq(Seq(isleOfManAdminBoundary), Seq(isleOfManIsland), Seq(middle), Seq(douglas)))
    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])

    Mockito.when(tagServiceMock.nameForOsmId(isleOfManAdminBoundary)).thenReturn(Some("Isle of Man"))
    Mockito.when(tagServiceMock.nameForOsmId(isleOfManIsland)).thenReturn(Some("Isle of Man"))
    Mockito.when(tagServiceMock.nameForOsmId(middle)).thenReturn(Some("Middle"))
    Mockito.when(tagServiceMock.nameForOsmId(douglas)).thenReturn(Some("Douglas"))
    Mockito.when(tagServiceMock.tagsFor(any[OsmId])).thenReturn(None)

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths)

    name must equalTo("Douglas, Middle, Isle of Man")
  }

  "need to consider overlappingareas" in {
    val unitedStates = OsmId(148838, R)
    val california = OsmId(165475, R)
    val mariposaCounty = OsmId(396465, R)
    val yosemite = OsmId(1643367, R)

    val mariposaPath = Seq(Seq(unitedStates), Seq(california), Seq(mariposaCounty))
    val yosemitePath = Seq(Seq(unitedStates), Seq(california), Seq(yosemite))

    val paths = Seq(mariposaPath, yosemitePath)
    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])

    Mockito.when(tagServiceMock.nameForOsmId(unitedStates)).thenReturn(Some("United States of America"))
    Mockito.when(tagServiceMock.nameForOsmId(california)).thenReturn(Some("California"))
    Mockito.when(tagServiceMock.nameForOsmId(mariposaCounty)).thenReturn(Some("Mariposa County"))
    Mockito.when(tagServiceMock.nameForOsmId(yosemite)).thenReturn(Some("Yosemite National Park"))
    Mockito.when(tagServiceMock.tagsFor(any[OsmId])).thenReturn(None)

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths)

    name must contain("Yosemite National park")
  }

}
