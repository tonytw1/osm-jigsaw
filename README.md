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


#### Extract interesting entities from the input file

To get the dataset down to managable size, extract any OSM entities which might represent closed areas into a working file.

Take all of the relations and the ways which are marked as closed.
Discard any entities which do not have name tags.
Collect the sub relations, ways and nodes which make up these entities.

This step might take the 1Gb Great Britain extract down to something more like 50Mb.


#### Resolve relations and closed ways into areas

Attempt to build closed areas from the filtered entities.
Relations may be composed multiple ways and sub relations which will need to be resolved.
A single relation might represent multiple areas (ie. a group of islands).

Ways within a relation may not always be in a sequential order.
Ways within an area's outline may be pointing in different directions.
A certain amount of trail and error might be needed.

Output the resolved areas in an unsorted file.


#### Sort the areas into a hierarchy

Much like a gravel sorting sieve. Smaller areas should fall down into larger areas. More important relations like countries should float to the top.
The resulting structure will look vaguely heap like with each child node presenting an area which fits inside it's parent.

ie. England be a child of United Kingdom after sorting. Bournemouth should be a descendant of England.

Output the graph as a file which can be sensibly parsed by a consumer.



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
