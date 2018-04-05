package input

import org.openstreetmap.osmosis.core.domain.v0_6._

trait TestValues {

  // Correctly formed with some convex bits
  val LONDON_BOROUGH_OF_RICHMOND_UPON_THAMES_RELATION = (151795L, EntityType.Relation)

  // Relation with out of order outer ways
  val TODMORDEN_RELATION = (7181767, EntityType.Relation)

  // Relation with offshore islands
  val NAYARIT = (7695827, EntityType.Relation)

  // Has several small island rings which could be trumping the main area
  val NEW_YORK_CITY = (175905L, EntityType.Relation)

  // Has a subarea
  val BOURNEMOUTH = (130884L, EntityType.Relation)

  // Western Isles - collection of islands
  // 1959008

  // Has two non overlapping rings
  //251411

  val NEW_ZEALAND = (556706L, EntityType.Relation)

  val ENGLAND = (58447, EntityType.Relation)
  val SOUTH_WEST_ENGLAND = (151339, EntityType.Relation)


  // 8057667WAY coal island - 2 ways which form a relationless area

  // Okmulgee 184191 broken relation -all inners
  // California (165475) first way is part of santa rosa island - hence the state ring is probably ignored
  // Cruz de los Esteros (5349509 Closed loop but ways have no roles

  // way 4557216 Salt Island
  // Part of many parent relations

  /* Europe - recursive loop
  Relation 6483281 has 2 relation members which are relations
  Recursing to resolve subrelation: RelationMember(Relation with id 5400348 in the role 'subarea')
  Relation 5400348 has 4 relation members which are relations
  Recursing to resolve subrelation: RelationMember(Relation with id 6483276 in the role 'subarea')
  Recursing to resolve subrelation: RelationMember(Relation with id 6483277 in the role 'subarea')
  Recursing to resolve subrelation: RelationMember(Relation with id 6483278 in the role 'subarea')
  Recursing to resolve subrelation: RelationMember(Relation with id 6483281 in the role 'subarea')
  Relation 6483281 has 2 relation members which are relations
  */
  val VILLA_NOVA_DA_BARQUINHA = (6483281L, EntityType.Relation) // Lists it's parent as a subarea

  // Known location points
  val london = (51.506, -0.106)
  val twickenham = (51.4282594, -0.3244692)
  val bournmouth = (50.720, -1.879)
  val lyndhurst = (50.8703, -1.5942)
  val edinburgh = (55.9518, -3.1840)
  val newport = (50.6995, -1.2935)
  val pembroke = (51.6756, -4.9122)
  val leeds = (53.7868, -1.5528)

  val dublin = (53.34261, -6.25411)
  val paris = (48.85433, 2.34859)
  val granada = (37.17700, -3.58966)

  val newYork = (40.7583041,-74.0038414)
  val halfDome = (37.7459620, -119.5330757)

}
