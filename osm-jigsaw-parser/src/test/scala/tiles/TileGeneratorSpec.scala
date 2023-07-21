package tiles

import org.scalatest.FlatSpec

class TileGeneratorSpec extends FlatSpec {

  private val tileGenerator = new TileGenerator()

  "TileGenerator" should
    "generate a tiles based on geohashes" in {
    assert(tileGenerator.generateTiles(1).size == 32)
    assert(tileGenerator.generateTiles(2).size == 1024)
    assert(tileGenerator.generateTiles(4).size == 1048576)
  }

}
