## None persisted OpenSteetMap geocoding spike

The OpenStreetMap data model is a fairly unstructured, flat format.
The implied structure comes from a loosely applied hierarchy of tags.
A full dataset contains around 5 billion entities and is in the region of 60Gb compressed and unindexed.

Gecoding is the art turning a location point into a human readable sentence (and vice versa).
(ie. 51.0, -0.3 <--> London, United Kingdon).

Nominatim is the default geocoding solution for OpenStreetMap.
It does are really great job of interpreting an implied structure and using it to construct sensible geocodings.

Nominatim uses a Postgres database as it's native data structure.
This can be operationally challenging. Important parts of the Nominatim code are implemented as a
Postgres module making it more differcult to alter or scale the application seperately from the data store.
Cloud deployments are prohibitively expensive.

Is it possible to approach this problem from a more stateless angle?
Can we transform a raw OSM data extract into a structured graph in application code without
having to import and persist the entire dataset?

- The input format should be OSM extract files (probably in the compressed pbf format)
- Happy live without partial updates so long as a full update can be performed in a sensible timeframe.
- Should be able to process a full planet file on a reasonable well equiped dev machine (say 32Gb of RAM).


### Proposed approach

- Extract top level relations =>

- Resolve these relations into bounding shapes =>

- Arrange these shapes into some sort of heap like structure; sift up the relations which enclose others to
try and extract a sensible hierarchy =>

- Pour remaining entites into the top of this structure and let them sift down to the smallest area which encloses them =>
(if this is polygon matching would using a GPU be sensible?)

This graph gives a path from each element back up to the the top level enclosing relation.
The graph can be tranversed to give a list of candidate components got the resolved placename;
the rendering decision can be deferred to run time.


### Considerations

- Try not to use local knowledge
ie. the system should infer that England and Wales are under United Kingdom and that Yosemite National Park is in Califonia from
the shape of the data rather than hardcoded rules.

- Try to defer decision making; try to avoid baking decisions into the structure too early; try to produce a
structure which allows for the rendering to vary at runtime.

ie.
London -> United Kingdom
London -> Greater London -> England -> United Kingdon.

Even if the former is the desired output, the graph should represent all of the possible paths (the later)
so as to not comprise the renderer's right to change it's mind at runtime.
