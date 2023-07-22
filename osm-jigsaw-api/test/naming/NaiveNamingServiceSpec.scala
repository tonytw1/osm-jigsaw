package naming

import com.esri.core.geometry.Point
import model.OsmId
import org.mockito.Matchers.any
import org.mockito.{Matchers, Mockito}
import org.specs2.mutable._
import tags.TagService

class NaiveNamingServiceSpec extends Specification {

  private val R = "R".charAt(0)
  private val W = "W".charAt(0)

  "place name is a concatenation of the enclosing area names" in {
    val australia = OsmId(80500L, R)
    val westernAustralia = OsmId(2316598, R)
    val ngaanyatjarra = OsmId(8165171, R)

    val paths = Seq(Seq(
      (Seq(australia), 0D),
      (Seq(westernAustralia), 0D),
      (Seq(ngaanyatjarra), 0D)
    ))

    val point = new Point(0, 0)

    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])

    Mockito.when(tagServiceMock.nameForOsmId(australia, point, None)).thenReturn(Some("Australia"))
    Mockito.when(tagServiceMock.nameForOsmId(westernAustralia, point, None)).thenReturn(Some("Western Australia"))
    Mockito.when(tagServiceMock.nameForOsmId(ngaanyatjarra, point, None)).thenReturn(Some("Ngaanyatjarra Indigenous Protected Area"))
    Mockito.when(tagServiceMock.tagsFor(any[OsmId], Matchers.eq(point))).thenReturn(None)

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths, point)

    name must equalTo("Ngaanyatjarra Indigenous Protected Area, Western Australia, Australia")
  }

  "consecutive duplicated place names are not adding value and can be removed" in {
    val isleOfManAdminBoundary = OsmId(62269, R)
    val isleOfManIsland = OsmId(6041206, R)
    val middle = OsmId(1061146, R)
    val douglas = OsmId(1061138, R)

    val paths = Seq(Seq(
      (Seq(isleOfManAdminBoundary), 0D),
      (Seq(isleOfManIsland), 0D),
      (Seq(middle), 0D),
      (Seq(douglas), 0D)
    ))

    val point = new Point(0, 0)

    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])

    Mockito.when(tagServiceMock.nameForOsmId(isleOfManAdminBoundary, point, None)).thenReturn(Some("Isle of Man"))
    Mockito.when(tagServiceMock.nameForOsmId(isleOfManIsland, point, None)).thenReturn(Some("Isle of Man"))
    Mockito.when(tagServiceMock.nameForOsmId(middle, point, None)).thenReturn(Some("Middle"))
    Mockito.when(tagServiceMock.nameForOsmId(douglas, point, None)).thenReturn(Some("Douglas"))
    Mockito.when(tagServiceMock.tagsFor(any[OsmId], Matchers.eq(point))).thenReturn(None)

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths, point)

    name must equalTo("Douglas, Middle, Isle of Man")
  }

  "need to consider overlapping areas" in {
    val unitedStates = OsmId(148838, R)
    val california = OsmId(165475, R)
    val mariposaCounty = OsmId(396465, R)
    val yosemite = OsmId(1643367, R)

    val mariposaPath = Seq(
      (Seq(unitedStates), 0D),
      (Seq(california), 0D),
      (Seq(mariposaCounty), 0D)
    )
    val yosemitePath = Seq(
      (Seq(unitedStates), 0D),
      (Seq(california), 0D),
      (Seq(yosemite), 0D)
    )

    val paths = Seq(mariposaPath, yosemitePath)

    val point = new Point(0, 0)

    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])

    Mockito.when(tagServiceMock.nameForOsmId(unitedStates, point, None)).thenReturn(Some("United States of America"))
    Mockito.when(tagServiceMock.nameForOsmId(california, point, None)).thenReturn(Some("California"))
    Mockito.when(tagServiceMock.nameForOsmId(mariposaCounty, point, None)).thenReturn(Some("Mariposa County"))
    Mockito.when(tagServiceMock.nameForOsmId(yosemite, point, None)).thenReturn(Some("Yosemite National Park"))
    Mockito.when(tagServiceMock.tagsFor(any[OsmId], Matchers.eq(point))).thenReturn(None)

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths, point)

    name must contain("Yosemite National Park")
  }

  "merging overlapping areas should preserve the ordering of nested areas" in {
    val unitedKingdom = OsmId(16689, R)
    val england = OsmId(16137, R)
    val southWestEngland = OsmId(151339, R)
    val dorset = OsmId(375535, R)
    val bournemouth = OsmId(42134, R)

    val viaEnglandPath = Seq(
      (Seq(unitedKingdom), 0D),
      (Seq(england), 0D),
      (Seq(dorset), 0D),
      (Seq(bournemouth), 0D)
    )
    val viaSouthWestEnglandPath = Seq(
      (Seq(unitedKingdom), 0D),
      (Seq(southWestEngland), 0D),
      (Seq(dorset), 0D),
      (Seq(bournemouth), 0D))

    val paths = Seq(viaEnglandPath, viaSouthWestEnglandPath)

    val point = new Point(0, 0)

    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])
    Mockito.when(tagServiceMock.nameForOsmId(unitedKingdom, point, None)).thenReturn(Some("United Kingdom"))
    Mockito.when(tagServiceMock.nameForOsmId(england, point, None)).thenReturn(Some("England"))
    Mockito.when(tagServiceMock.nameForOsmId(southWestEngland, point, None)).thenReturn(Some("South West England"))
    Mockito.when(tagServiceMock.nameForOsmId(dorset, point, None)).thenReturn(Some("Dorset"))
    Mockito.when(tagServiceMock.nameForOsmId(bournemouth, point, None)).thenReturn(Some("Bournemouth"))
    Mockito.when(tagServiceMock.tagsFor(any[OsmId], Matchers.eq(point))).thenReturn(None)

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths, point)

    name must equalTo("Bournemouth, Dorset, England, South West England, United Kingdom")
  }

  "should excluded entities which have tags which do not contribute to place names" in {
    val ireland = OsmId(62273, R)
    val dublinCity1954 = OsmId(6741826, R)
    val dublin = OsmId(5576531, R)

    val path = Seq(
      (Seq(ireland), 0D),
      (Seq(dublinCity1954), 0D),
      (Seq(dublin), 0D)
    )

    val paths = Seq(path)

    val point = new Point(0, 0)

    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])
    Mockito.when(tagServiceMock.nameForOsmId(ireland, point, None)).thenReturn(Some("Ireland"))
    Mockito.when(tagServiceMock.nameForOsmId(dublinCity1954, point, None)).thenReturn(Some("Dublin City 1953"))
    Mockito.when(tagServiceMock.nameForOsmId(dublin, point, None)).thenReturn(Some("Dublin"))

    Mockito.when(tagServiceMock.tagsFor(any[OsmId], Matchers.eq(point))).thenReturn(None)

    val historicTags = Map[String, String] {
      "historic" -> "yes"
    }
    Mockito.when(tagServiceMock.tagsFor(OsmId(6741826, R), point)).thenReturn(Some(historicTags))

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths, point)

    name must equalTo("Dublin, Ireland")
  }

  "sorting by areas size is a way to bring outlying paths into line" in {
    val spain = OsmId(1311341, R)
    val andalusia = OsmId(349044, R)
    val almeria = OsmId(348997, R)
    val yahooAlmeria = OsmId(77344161, W)

    val normalPath = Seq(
      (Seq(spain), 59.080443847178266),
      (Seq(andalusia), 8.92241984358786),
      (Seq(almeria), 0.029915336745489712)
    )
    val outlinerPath = Seq(
      (Seq(spain), 59.080443847178266),
      (Seq(yahooAlmeria), 0.18624327963594123)
    )

    val paths = Seq(normalPath, outlinerPath)

    val point = new Point(0, 0)

    val tagServiceMock = org.mockito.Mockito.mock(classOf[TagService])
    Mockito.when(tagServiceMock.nameForOsmId(spain, point, None)).thenReturn(Some("Spain"))
    Mockito.when(tagServiceMock.nameForOsmId(andalusia, point, None)).thenReturn(Some("Andalusia"))
    Mockito.when(tagServiceMock.nameForOsmId(almeria, point, None)).thenReturn(Some("Almeria"))
    Mockito.when(tagServiceMock.nameForOsmId(yahooAlmeria, point, None)).thenReturn(Some("Almeria"))
    Mockito.when(tagServiceMock.tagsFor(any[OsmId], Matchers.eq(point))).thenReturn(None)

    val namingService = new NaiveNamingService(tagServiceMock)

    val name = namingService.nameFor(paths, point)

    name must equalTo("Almeria, Andalusia, Spain")
  }

  /*
  "When naming an area with overlapping relations prefer localised name tags" in {
    //https://www.openstreetmap.org/relation/51477
    //https://www.openstreetmap.org/relation/4108738#map=7/51.351/10.454
    //Germany, not 'Deutschland, Germany'
    failure
  }
  */


  /*
 "for nodes with multiple entites, an exclusion should only remove the effected entity not the entire node" in {
   failure
 }
 */

}
