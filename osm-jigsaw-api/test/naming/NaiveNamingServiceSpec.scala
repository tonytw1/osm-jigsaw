package naming

import model.OsmId
import org.mockito.Mockito
import org.specs2.mutable._
import tags.TagService

class NaiveNamingServiceSpec extends Specification  {

  "place name is a concatenation of the the enclosing area names" in {
    val australia = OsmId(80500L, "R".charAt(0))
    val westernAustralia = OsmId(2316598, "R".charAt(0))
    val ngaanyatjarra = OsmId(8165171, "R".charAt(0))

    val paths = Seq(Seq(Seq(australia), Seq(westernAustralia), Seq(ngaanyatjarra)))
    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])

    Mockito.when(tagServiceMock.nameForOsmId(australia)).thenReturn(Some("Australia"))
    Mockito.when(tagServiceMock.nameForOsmId(westernAustralia)).thenReturn(Some("Western Australia"))
    Mockito.when(tagServiceMock.nameForOsmId(ngaanyatjarra)).thenReturn(Some("Ngaanyatjarra Indigenous Protected Area"))
    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths)

    name must equalTo("Ngaanyatjarra Indigenous Protected Area, Western Australia, Australia")
  }

  "consecutive duplicated place names are not adding value and can be removed" in {
    val isleOfManAdminBoundary = OsmId(62269, "R".charAt(0))
    val isleOfManIsland = OsmId(6041206, "R".charAt(0))
    val middle = OsmId(1061146, "R".charAt(0))
    val douglas = OsmId(1061138, "R".charAt(0))

    val paths = Seq(Seq(Seq(isleOfManAdminBoundary), Seq(isleOfManIsland), Seq(middle), Seq(douglas)))
    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])

    Mockito.when(tagServiceMock.nameForOsmId(isleOfManAdminBoundary)).thenReturn(Some("Isle of Man"))
    Mockito.when(tagServiceMock.nameForOsmId(isleOfManIsland)).thenReturn(Some("Isle of Man"))
    Mockito.when(tagServiceMock.nameForOsmId(middle)).thenReturn(Some("Middle"))
    Mockito.when(tagServiceMock.nameForOsmId(douglas)).thenReturn(Some("Douglas"))
    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths)

    name must equalTo("Ngaanyatjarra Indigenous Protected Area, Western Australia, Australia")
  }

}
