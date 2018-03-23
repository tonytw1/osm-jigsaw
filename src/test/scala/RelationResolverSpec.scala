import com.esri.core.geometry._
import input.sinks.OsmEntitySink
import input.{OsmReader, TestValues}
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec
import resolving.RelationResolver

import scala.collection.JavaConverters._
import scala.collection.mutable

class RelationResolverSpec extends FlatSpec with TestValues with EntityRendering {

  val deferencedOutputFile = "/tmp/out.pbf"

  "relation resolver" should "build bounding areas for each extracted relation" in {
    def all(entity: Entity): Boolean  = true

    val allFound = mutable.Buffer[Entity]()
    def addToFound(entity: Entity) = allFound.+=:(entity)

    val sink = new OsmEntitySink(all, addToFound)
    new OsmReader(deferencedOutputFile, sink).read

    val relations: Set[Relation] = allFound.flatMap { e =>
      e match {
        case r: Relation => Some(r)
        case _ => None
      }
    }.toSet

    val ways: Map[Long, Way] = allFound.flatMap { e =>
      e match {
        case w: Way => Some(w)
        case _ => None
      }
    }.map { i =>
      (i.getId, i)
    }.toMap

    val nodes = allFound.flatMap { e =>
      e match {
        case n: Node => Some(n)
        case _ => None
      }
    }.map { i =>
      (i.getId, i)
    }.toMap


    val allRelations = relations.map( r => (r.getId, r)).toMap  // TODO Does this contain all of the subrelations?

    println("Found " + relations.size + " relations to process")

    val relationResolver = new RelationResolver()
    val boundedRelations = relationResolver.resolve(relations, allRelations, ways, nodes)
    println(boundedRelations)

    Seq(london, twickenham, bournmouth, lyndhurst, edinburgh, newport, pembroke, leeds, newYork, halfDome).map { location =>
      val components = Range(1, 11).flatMap { i =>
        val bound = boundedRelations.find { b =>
          val r = b._1
          val adminLevel = r.getTags.asScala.find(t => t.getKey == "admin_level").map(t => t.getValue)

          val area = b._2
          val pt = new Point(location._1, location._2)


          val sr: SpatialReference = SpatialReference.create(1)
          val contains: Boolean = OperatorContains.local().execute(area, pt, sr, null)

          adminLevel == Some(i.toString) && contains
        }
        bound
      }.map(_._1).reverse

      println(location + ": " + components.map(r => render(r)).mkString(", "))
    }

    succeed
  }

}
