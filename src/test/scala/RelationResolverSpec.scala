import java.io.{FileOutputStream, ObjectOutputStream}

import input.{SinkRunner, TestValues}
import model.Area
import org.openstreetmap.osmosis.core.domain.v0_6._
import org.scalatest.FlatSpec
import resolving.RelationResolver

import scala.collection.mutable

class RelationResolverSpec extends FlatSpec with TestValues with EntityRendering {

  val deferencedOutputFile = "/tmp/out.pbf"
  var areasOutputFile = "/tmp/areas.ser"

  "relation resolver" should "build bounding areas for each extracted relation" in {
    def all(entity: Entity): Boolean  = true

    val allFound = mutable.Buffer[Entity]()
    def addToFound(entity: Entity) = allFound.+=:(entity)
    new SinkRunner(deferencedOutputFile, all, addToFound).run

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

    val areas: Set[Area] = relationResolver.resolve(relations, allRelations, ways, nodes)
    println("Produced " + areas.size + " relation shapes")

    // TODO serialize to disk for quick interation of the next step
    val oos = new ObjectOutputStream(new FileOutputStream(areasOutputFile))
    oos.writeObject(areas)
    oos.close
    println("Dumped areas to file: " + areasOutputFile)

    succeed
  }

}
