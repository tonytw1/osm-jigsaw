## OpenStreetMap Jigsaw parser

Parses an OSM protocol buffer extract and produces a sorted graph of the areas found in that extract.


### Usage

#### Build

This is a Scala / sbt project. Install sbt and build a jar file.

```
sbt clean assembly
```

#### Processing an extract file

Obtain a planet.osm extract (using the protocol buffer .pbf format).

We run the extract file through the following steps to arrive at a sorted graph of the areas found in the extract:


##### 1) Boundaries

The extract file appears to be grouped by entity type; it lists all Nodes then Ways and then all Relations (*caution [Hyrum's Law](https://www.hyrumslaw.com)).

Been able to seek to the start of different types of entities is useful so scan the extract file and record the offsets for the edges between different record types.

```
malta-230704.boundaries.json
{
    "Node":189,
    "Relation":5573612,
    "EOF":5706583,
    "Way":3194269,
    "Bound":0
}
```

##### 2) Extract area entities

Read the extract file and extract the entities from which could be parts of areas.

- All of the named relations and their sub relations.
- All of the named and closed ways.
- The ways and nodes which are parts of the above.

Extract to separate relations, ways and nodes files; with the ways and nodes been indexed by id.

This produces 3 new files:

```
new-zealand-230705.rels.pbf
new-zealand-230705.rels.pbf.nodes.vol
new-zealand-230705.rels.pbf.ways.vol
```

##### 3) Resolve Areas ways

Resolve the areas formed by the extracted relations and closed ways.


For relations which involves finding a combination of it's ways which can be arranged nose to tail in a way which uses all of the ways and forms a closed loop.
All sub relations need to be expanded so that the entire relation is described as a set of ways.

I'm quite surprised that this actually works and is extremely fast (it's probably O(n^2) but n is small for a given relation).

Outputs a list of `OutputArea's which describe sequence of ways which make up a closed area.

```
new-zealand-230705.areaways.pbf
```

Then resolve the nodes which make up the ways to produce the outlines of the areas.
Outputs a list `OutputWay`s which contain the points which make up those ways.

```
new-zealand-230705.areaways.pbf.ways.pbf
```


#### 4) Build areas

Remaps the OutputWays into `OutputArea`s.
Deduplicates areas with identical shapes and merges their OSM ids.

Outputs a file of `OutputArea`s describing all the polygons found in the extract.

```
new-zealand-230705/new-zealand-230705.areas.pbf
```

#### 5) Sort the areas into a graph

Given the polygons representing the area shapes found in the extract, sort them into a graph where every area fits inside it's parent.
ie. England fixes inside the UK, which fits inside Europe, which fits inside the World.

The step may take some time (approximately 14 hours for a full planet extract).

`new-zealand-230705.graph.pbf`

#### 6) Flip the graph

The graph produced in the previous step is formatted as nodes with parents; nodes with children is actually more useful 
to the consuming app and can be represented in a more compact format.
Read the graph and invert it into a new file.

`new-zealand-230705.graphv2.pbf`


#### 7) Extract the tags for areas

Extracts the OSM tags for the entities which produced areas. This allows names for the areas to be derived at runtime.


On completion we should have 3 files representing the extracted, sorted areas.

```
ireland-and-northern-ireland-180717.areas.pbf
ireland-and-northern-ireland-180717.graph.pbf
ireland-and-northern-ireland-180717.tags.pbf
```


#### 8) Tiling

The full planet graph, area polygons and OSM tag data for those areas can be read entirely into a 64Gb heap.
This isn't completely out of order and would probably be an ok value proposition if the API had constant use.

But it's only a demo, so we'd like to optimise for occasional user / low traffic / low standing cost.
Standing memory usage is the most expensive problem.

Requests from a given user are likely to be for areas which are close to each other.
Segmenting the graph into tiles which cover small areas of the graph should allow us to service occasional users without having to retain the entire graph in memory.

We're happy to accept duplication of data between tiles to achieve this smaller memory footprint.

Untiled disk usage:
```
13G Jul 12 11:29 planet-230703.areas.pbf
185M Jul 15 17:44 planet-230703.graphv2.pbf
2.3G Jul 13 11:44 planet-230703.tags.pbf
```

Tilted disk usage:
```
du -h
83G .
```

```
-rw-rw-r-- 1 tony tony   4795094 Jul 22 09:06 planet-230703.areas-dws.pbf
-rw-rw-r-- 1 tony tony   4795094 Jul 22 09:04 planet-230703.areas-dwt.pbf
-rw-rw-r-- 1 tony tony   4795094 Jul 22 09:04 planet-230703.areas-dwu.pbf
-rw-rw-r-- 1 tony tony   4795094 Jul 22 09:05 planet-230703.areas-dwv.pbf
```

```
sha256sum planet-230703.areas-dws.pbf planet-230703.areas-dwt.pbf
94dcd56575779c1a36e53a5f6364dcc6650269309a2b15bea4495f52535ef944  planet-230703.areas-dws.pbf
94dcd56575779c1a36e53a5f6364dcc6650269309a2b15bea4495f52535ef944  planet-230703.areas-dwt.pbf
```

```
rdfind .

It seems like you have 40374 files that are not unique
Totally, 12 GiB can be reduced.
```