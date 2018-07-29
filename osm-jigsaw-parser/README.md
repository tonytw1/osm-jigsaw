### Usage

#### Compiling
This is a Scala / sbt project. Install sbt and build a jar file.

```
sbt clean assembly
```


#### Processing an extract file

Obtain a planet.osm extract (using the protocol buffer .pbf format).

##### 1) Split the extract file

An OSM extract seems to be ordered by nodes, ways and then relations. This is not the optimal ordering for our workflow.
There are gains to be had by splitting the extract file by entity type to save time spent scanning past entities we are not interested in.

```
input="ireland-and-northern-ireland-180717"
jarfile="target/scala/osm-jigsaw-assembly-0.1.0-SNAPSHOT.jar"
java -jar $jarfile -s split $input.osm.pbf
```

This should produce 3 new files:

```
ireland-and-northern-ireland-180717.osm.pbf.nodes
ireland-and-northern-ireland-180717.osm.pbf.relations
ireland-and-northern-ireland-180717.osm.pbf.ways
```


##### 2) Extract the entities which represent areas

```
java -Xmx16G -jar $jarfile -s extract $input.osm.pbf $input.rels.pbf
```

The heap sizes have been chosen for a full planet extract on a machine with 32Gb of RAM.

This step should produce 3 more files:

```
ireland-and-northern-ireland-180717.rels.pbf
ireland-and-northern-ireland-180717.rels.pbf.nodes.vol
ireland-and-northern-ireland-180717.rels.pbf.ways.vol
```

#### 3) Build areas from the extracted entities

```
java -Xmx16G -jar $jarfile -s areas $input.rels.pbf $input.areas.pbf
```

#### 4) Sort the areas into a graph

```
java -Xmx28G -jar $jarfile -s graph $input.areas.pbf $input.graph.pbf
```

The step may take some time (approximately 4 hours for a full planet extract).


#### 5) Extract the tags for areas

Extracts the OSM tags for the entities which produced areas. This allows names for the areas to be derived at runtime.

```
java -jar $jarfile -s tags $input.rels.pbf $input.tags.pbf
```

On completion you should have 3 files representing the extracted, sorted areas.

```
ireland-and-northern-ireland-180717.areas.pbf
ireland-and-northern-ireland-180717.graph.pbf
ireland-and-northern-ireland-180717.tags.pbf
```
