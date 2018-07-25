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

}
