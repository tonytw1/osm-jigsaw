## OpenStreetMap Jigsaw

Gecoding is the art turning a location point into a human readable name (and vice versa).
(ie. 51.0, -0.3 <--> London, United Kingdon).

Nominatim is the default OpenStreetMap geocoding solution.
It does are really great job of interpreting an implied structure and using it to construct sensible geocodings.

Nominatim uses a Postgres database. This can be operationally challenging for a number of reasons, including:

- A full import required alot of storage (~1TB of SSD disk)
- An initial import of the full dataset can take a long time (days).
- Important parts of the Nominatim code are implemented as a Postgres module making it more differcult to alter.
- Cloud deployments are prohibitively expensive.

Is it possible to approach this problem from a more stateless angle?
Can we transform a raw OSM data extract into a structured graph in application code without needing an imported database?


### Considerations

The OpenStreetMap data model is a fairly unstructured, flat format. The implied structure comes from a loosely applied hierarchy of tags.
The full dataset contains around 5 billion entities is in the region of 60Gb compressed.

These considerations come to mind:

- Minimise processing by using the existing OSM extract format as input (likely the compressed .pbf format

- Try not to use local knowledge. 
ie. The system should infer that England and Wales are inside the United Kingdom and that Yosemite National Park is in Califonia from the shape of the data rather than hardcoded rules or
human intervention.

- Try to defer decision making; try to avoid discarding information or baking decisions into the structure too early; try to produce a structure which allows for the rendering to vary at runtime.

    ie. There could be multiple valid representions where an areas sits.
    London -> United Kingdom
    London -> Greater London -> England -> United Kingdon.

    Even if the former is the desired output, the graph should represent all of the possible paths so that the consumer is free to change it's mind at runtime.

- We're happy to live without partial updates so long as a full update can be performed in a reasonable timeframe (hours not days).

- It should be possible to process a full planet file on a reasonably well equipped developer machine (say 32Gb of RAM).


### Proposed approach

Starting with a raw OSM extract file, preform a number of independant steps to transform the extract into the desired output format.


#### 1) Extract interesting entities from the input file

To get the dataset down to managable size, extract any OSM entities which might represent areas into a working file.

Take all of the relations and the ways which are marked as closed.
Discard any entities which do not have name tags.
Collect the sub relations, ways and nodes which make up these entities.

Some relations have sub relations which form circular references; we should ignore these.

This step might take the 1Gb Great Britain extract down to something more like 50Mb.


#### 2) Resolve relations and closed ways into areas

Attempt to build closed shapes from the filtered entities.

Relations may be composed multiple ways and sub relations which will need to be resolved.
A single relation might represent multiple areas (ie. a group of islands).

Ways within a relation may not always be in a sequential order. 
Ways within an area's outline may be pointing in different directions. A certain amount of trail and error might be needed.

Output the resolved areas in an unsorted file.


#### 3) Sort the areas into a hierarchy

Much like a gravel sorting sieve. Smaller areas should fall down into larger areas. More important relations like countries should float to the top.
The resulting structure will look vaguely like a heap with each child node representing an area which fits inside it's parent.

ie.
England should be a child of United Kingdom after sorting.
Bournemouth should be a descendant of England.

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
