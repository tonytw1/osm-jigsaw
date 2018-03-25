package input

import org.openstreetmap.osmosis.core.domain.v0_6._

trait TestValues {

  // Correctly formed with some convex bits
  val LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION = (151795, EntityType.Relation)

  // Relation with out of order outer ways
  val TODMORDEN_RELATION = (7181767, EntityType.Relation)

  // Relation with offshore islands
  val NAYARIT = (7695827, EntityType.Relation)

  // Has several small island rings which could be trumping the main area
  val NEW_YORK_CITY = (175905, EntityType.Relation)

  // Okmulgee 184191 broken relation -all inners
  // California (165475) first way is part of santa rosa island - hence the state ring is probably ignored
  // Cruz de los Esteros (5349509 Closed loop but ways have no roles

  // Known location points
  val london = (51.506, -0.106)
  val twickenham = (51.4282594, -0.3244692)
  val bournmouth = (50.720, -1.879)
  val lyndhurst = (50.8703, -1.5942)
  val edinburgh = (55.9518, -3.1840)
  val newport = (50.6995, -1.2935)
  val pembroke = (51.6756, -4.9122)
  val leeds = (53.7868, -1.5528)

  val newYork = (40.7583041,-74.0038414)
  val halfDome = (37.7459620, -119.5330757)

}
