## OpenStreetMap Jigsaw

A shape based approach to geocoding with OpenStreetMap extracts.

This system extracts every area shape from an OpenStreetMap extract and sorts them into a graph.

The graph of areas is used to [infer places name for points](#inferring-location-names).

This graph is exported as a [set of Protocol buffer files](#output-files) and exposed as a [JSON API](osm-jigsaw-api).
As a side effect we can detect broken relations in the extract file which cycle back on themselves.

With tiling, the API can serve the full planet graph of ~15 million areas from 4Gb of memory.

A [full planet demo is viewable here](https://osm-jigsaw.eelpieconsulting.co.uk).


### Background

Gecoding is the art (not science!) of turning a location point into a human readable name (and vice versa).
(ie. 51.0, -0.3 <--> London, United Kingdom).

[Nominatim](https://wiki.openstreetmap.org/wiki/Nominatim) is the default OpenStreetMap geocoding solution.

It does a really great job of inferring structure from the geometry and tagging of Open Street Map elements.

Nominatim uses a Postgres database which indexes the entire planet extract.
It's a serious pick of infrastructure, using TB's of disk and 10s of Gbs of memory.

What would happen if we approached this problem from a solely shape based angle?
Do the shapes of the elements in the OSM have enough structure to infer meaningful location names?

Can we transform a raw OSM data extract directly into a structured graph without having to import it into a database?


### Considerations

The OpenStreetMap data model is a fairly unstructured, flat format. 
It consists of primitive elements: nodes (points), ways (paths of points) and relations (compositions of nodes, ways and other relations)

The full dataset contains around 5 billion elements and is in the region of 70Gb compressed.

These considerations come to mind:

- Minimise preprocessing by using the existing OSM extract format as input (likely the compressed .pbf format)

- Try not to use local knowledge. 
ie. The system should infer that England and Wales are inside the United Kingdom and that Yosemite National Park is in California from the shapes of these elements only.

- Try to defer decisions. Avoid discarding information or baking decisions into the structure too early. Try to produce a structure which allows for the rendering to vary at runtime.

    ie. There could be multiple valid representations of where an areas sits.
    London -> United Kingdom
    London -> Greater London -> England -> United Kingdom.

    Even if the former is the desired output, the graph should represent all the possible paths so that the consumer is free to decide at runtime.

- It should be possible to process a full planet file on a reasonably well equipped developer machine (say 32Gb of RAM).


### Proposed approach

Starting with a raw OSM extract file, preform a number of incremental transformations until we have a sorted graph of areas which fit inside their parents.



#### 1) Extract interesting elements

To get the dataset down to manageable size, extract any OSM elements which might represent shapes.

- Take all of the relations and the closed ways.
- Discard any elements which do not have name tags.
- Collect the sub relations, ways and nodes which make up these elements.

Some relations have sub relations which form circular references; we should ignore these.


#### 2) Resolve relations and closed ways into areas

Attempt to build closed shapes from the filtered elements.

- Relations may be composed of multiple ways and sub relations which will need to be resolved.
- A single relation could represent multiple areas (ie. a group of islands).
- Ways within a relation might not always be in sequential order. 
- Ways within an relation outline may be pointing in different directions. A certain amount of trail and error might be needed.

Output the resolved areas into an unsorted file.


#### 3) Sort the areas into a graph

Much like a child's shape sorting game. Smaller areas should fall down into larger areas with they fit inside. 
More important relations like countries should float to the top.
The resulting structure will look vaguely like a heap with each child node representing an area which fits inside it's parent.

ie.
England should be a child of United Kingdom after sorting.
Bournemouth should be a child of England.

Output the graph in a format which can be sensibly parsed by a consumer.



### OSM Jigsaw parser

The [OSM Jigsaw parser](osm-jigsaw-parser) takes an OSM protocol buffer extract as input, preforms the steps described above and outputs the files described below.



### Output files

The 3 output files are in protocol buffer format and contain [OutputArea](osm-jigsaw-parser/src/main/protobuf/outputarea.proto), [OutputGraphNodeV2](osm-jigsaw-parser/src/main/protobuf/outputgraphnodev2.proto) and [OutputTagging](osm-jigsaw-parser/src/main/protobuf/outputtagging.proto) objects.
These formats are described below.
These 3 files should be placed in a location where they are accessible to the [OSM Jigsaw API](osm-jigsaw-api).

Protocol buffer was chosen for it's relatively small file size and fast import; it's also consistent with the OSM extract files.


#### OutputArea

Describes and areas extracted from an OSM relation or way.

| Field      | Type           | Description                                                                                                                                                   |
|------------|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| id         | Long           | A disposable id which can be used to reference this area when assembling the graph. This id is not likely to remain the same for a particular area over time. |
| osm_ids    | List of String | A list of the OSM ids for elements which have this area. ie. 123W, 456W                                                                                       |
| latitudes  | List of Double | A list of the latitudes of the points which form the outline for this area.                                                                                   |
| longitudes | List of Double | A list of the longitudes of the points which form the outline for this area.                                                                                  |
| area       | Double         | The relative size of the area.                                                                                                                                |


#### OutputGraphNodeV2

Describes a graph node in node with children format.

| Field    | Type         | Description                                  |
|----------|--------------|----------------------------------------------|
| area     | Long         | The id of the area which occupies this node. |
| children | List of Long | The ids of the child nodes of this node.     |


#### OutputTagging

Represents the OSM tags for an OSM id.

| Field  | Type           | Description                                   |
|--------|----------------|-----------------------------------------------|
| osm_id | String         | The OSM id<br/> these tags apply to. ie. 123R |
| keys   | List of String | The tag keys. ie. name:en                     |
| values | List of String | The tag values. ie. England                   |


### Execution

A full extract runs to completion on a machine with 64Gb of heap space in approximately 14 hours, producing a graph of 15 million areas.

The graph sorting algorithm has a n*m cost. It works very well when most nodes fall down into a larger node.
Some problematic nodes collect large numbers of children (>100,000) at the top level which will not drop down.
These nodes waste alot of run time but do (just barely) run to compilation.

This graph is then exposed via a [JSON API](osm-jigsaw-api).
The full graph can be loaded into a JVM with 64Gb of heap. If tiled it will run from a 4Gb heap.

The [API](osm-jigsaw-api) can typically resolve the areas enclosing a point in ~ 100ms.



### Inferring location names

Given a point, it is a fairly fast operation start at the root of the graph and step down the hierarchy of nested areas,
gathering all the possible paths down to the smallest areas enclosing the point.

We can try to infer a place name from this collection of paths.

- As each area in the graph was derived from an OSM entity (a relation or a closed way), we can inspect the OSM tags for each area.
This should give a label for each node on the path.

- There will be elements in the path which are not relevant such as time zones, electoral boundaries and historical data; these can be ignored.

- Useful components of the name may come from more than one of the paths.

- We can probably ignore adjacent elements with the same name.
 ie. New Zealand, Wellington, Wellington

- Transform the into a localised output
In english this means collecting the name:en tags and joining the smallest to largest.
ie. Bournemouth, England, United Kingdom.


A [naive implementation is provided](osm-jigsaw-api/app/naming/NaiveNamingService.scala) in the API.


### Results

This approach to geocoding does well for some use cases and less so for others.
Here's some examples.

| Location | Outcome |
| ------------- | ------------- |
| Richmond Park | Nicely illustrates that Richmond Park is a large area which falls across multiple London boroughs. |
| Twickenham Rowing Club | Correctly places the rowing club on the Eel Pie Island matching it's colloquial address. |
| Yosemite National Park | Correctly placed in California. |
| Perth, Australia | The lack of an enclosing city area means that Perth is not mentioned in results. | 
| Bournemouth Pier | An interesting outlier. The pier sits just outside of the local authority and county boundaries, losing locality. |
