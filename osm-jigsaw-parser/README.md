## OpenStreetMap Jigsaw

Parses an OSM protocol buffer extract and produces a sorted graph of areas.


### Usage

#### Build

This is a Scala / sbt project. Install sbt and build a jar file.

```
sbt clean assembly
```

#### Processing an extract file

Obtain a planet.osm extract (using the protocol buffer .pbf format).


##### 1) Boundaries

The extract file appears to be grouped by entity type; it lists all Nodes then Ways and then all Relations (*caution [Hyrum's Law](https://www.hyrumslaw.com)).

Been able to seek to the start of different types of entities is useful so scan the extract file and record the offsets for the edges between different record types.


##### 2) Extract area entities

Extract the entities from which could be parts of areas.

All of the named relations and their sub relations.
All of the named and closed ways.
And of the ways and nodes which are parts of the above.

Extract to separate relations, ways and nodes files; with the ways and nodes been indexed by id.

```
input="ireland-and-northern-ireland-180717"
jarfile="target/scala/osm-jigsaw-assembly-0.1.0-SNAPSHOT.jar"
java -jar $jarfile -s split $input.osm.pbf
```

This produces 3 new files:

```
new-zealand-230705.rels.pbf
new-zealand-230705.rels.pbf.nodes.vol
new-zealand-230705.rels.pbf.ways.vol
```

##### 3) Resolve Areas

Resolve the areas formed by the extracted relations and closed ways.

For relations which involves finding a combination of it's ways which can be arranged nose to tail 
in a way which uses all of the ways and forms a closed loop.
I'm quite surprised that this actually works and is extremely fast (it's probably O(n^2) but n is small for a given relation).

Outputs a list of `OutputArea's which describe sequence of ways which make up a closed area.

Resolve the way points from the nodes which make up these ways.
Output a list `OutputWay`s which contain the points which make up those ways.

```
new-zealand-230705.areaways.pbf
new-zealand-230705.areaways.pbf.ways.pbf
```

#### 4) Build areas

Render and duplicate.

Render areas described as a sequence of ways into polygon outlines made up of points.
Deduplicate areas with identical shapes and merge there OSM ids.

Output an areas describing all the closed area polygons found in the extract.

```
new-zealand-230705/new-zealand-230705.areas.pbf
```




```
java -Xmx16G -jar $jarfile -s areas $input.rels.pbf $input.areas.pbf
```

#### 5) Sort the areas into a graph

Given the polygons representing the area shapes found in the extract, sort them into a graph where every area fits inside it's parent.
ie. England fixes inside the UK, which fits inside Europe, which fits inside the World.


```
java -Xmx28G -jar $jarfile -s graph $input.areas.pbf $input.graph.pbf
```

The step may take some time (approximately 14 hours for a full planet extract).


#### 6) Flip the graph



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
