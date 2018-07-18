## OpenStreetMap Jigsaw

Gecoding is the art turning a location point into a human readable sentence (and vice versa).
(ie. 51.0, -0.3 <--> London, United Kingdon).

The OpenStreetMap data model is a fairly unstructured, flat format. The implied structure comes from a loosely applied hierarchy of tags.
The full dataset contains around 5 billion entities and is in the region of 60Gb compressed and unindexed.

Nominatim is the default geocoding solution for OpenStreetMap.
It does are really great job of interpreting an implied structure and using it to construct sensible geocodings.

Nominatim uses a Postgres database as it's native data structure.
This can be operationally challenging for a number of reasons, including:

- A full import required alot of storage (~1TB of SSD disks)
- An initial import of the full dataset can take along time (days).
- Important parts of the Nominatim code are implemented as a Postgres module making it more differcult to alter or scale the application seperately from the data store.
- Cloud deployments are prohibitively expensive.

Is it possible to approach this problem from a more stateless angle?
Can we transform a raw OSM data extract into a structured graph in application code without an imported database?



### Considerations

- Try not to use local knowledge. 
ie. The system should infer that England and Wales are inside the United Kingdom and that Yosemite National Park is in Califonia from the shape of the data rather than hardcoded rules or
human intervention.

- Try to defer decision making; try to avoid discarding information or baking decisions into the structure too early; try to produce a structure which allows for the rendering to vary at runtime.

ie. There could he multiple valid representions where an areas sits.

London -> United Kingdom
London -> Greater London -> England -> United Kingdon.

Even if the former is the desired output, the graph should represent all of the possible paths so that the consumer is free to change it's mind at runtime.


- Minimise processing by using the existing OSM extract format as input (likely the compressed .pbf format

- We're happy to live without partial updates so long as a full update can be performed in a reasonable timeframe (hours not days).

- It should be possible to process a full planet file on a reasonably well equiped developer machine (say 32Gb of RAM).


### Proposed approach

Starting with a raw OSM extract file, preform a number of independant steps to transform the extract into the desired output format.



- Extract high level entities and their components =>

Select a high level set of entities; say admin boundary relations.
Extract these from an OSM extract file. Resolve the sub relations, ways and nodes which they are composed of and dump
these out into a smaller working file.

This step might take the 1Gb Great Britain extract down to something more like 50Mb.


- Resolve these relations into bounding areas =>

Load the high levek entities and attempt to build bounding areas for them.
Relations may be composed of many shapes. Ways and nodes don't always appear into order.


- Try to sort these areas into some sort of hierarchy

Much like a gravel sorting sieve.
Smaller areas should falled down inside larger areas.
More important relations like countries should float to the top regardless of their tags.
Ignore overlapping areas for now; does this mean than insertion order becomes important?


- Iterate, pouring remaining entites into the top of this structure in batches

Let them sift down to the smallest area which encloses them =>
(if this is polygon matching would using a GPU be sensible?)

This graph gives a path from each element back up to the the top level enclosing relation.
The graph can be tranversed to give a list of candidate components got the resolved placename;
the rendering decision can be deferred to run time.





### Progress

22 April - Full extract runs to completion producing 8.5 million areas (all named relations, and named closed ways).
Resulting graph requires 50Gb of heap to load. 
 
```
java -Xmx24G -jar osm-jigsaw-assembly-0.1.0-SNAPSHOT.jar -s split great-britain-latest.osm.pbf
java -Xmx24G -jar osm-jigsaw-assembly-0.1.0-SNAPSHOT.jar -s extract great-britain-latest.osm.pbf great-britain-latest.rels.pbf
java -Xmx24G -jar osm-jigsaw-assembly-0.1.0-SNAPSHOT.jar -s areas great-britain-latest.rels.pbf great-britain-latest.areas.ser
java -Xmx24G -jar osm-jigsaw-assembly-0.1.0-SNAPSHOT.jar -s graph great-britain-latest.areas.ser great-britain-latest.graph.ser
```




```
sbt clean assembly
```
