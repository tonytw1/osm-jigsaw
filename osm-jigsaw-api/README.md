## OpenStreetMap Jigsaw API

A JSON API for the area graph produced by OpenStreetMap Jigsaw parser.

### Endpoints

#### GET /reverse

Given a latitude / longitude point return a list of possible paths down to that point.

| Parameter | Description |
| ------------- | ------------- |
| lat           | Latitude of point |
| lon           | Longitude of point |


```
/reverse?lat=51.4454933&lon=-0.3254827
```

Example response
```
[
  [
    {
      "id": 22277,
      "entities": [
        {
          "osmId": "80500R",
          "name": "Australia"
        }
      ],
      "children": 98
    },
    {
      "id": 329799,
      "entities": [
        {
          "osmId": "2316598R",
          "name": "Western Australia"
        }
      ],
      "children": 3
    },
    {
      "id": 1063321,
      "entities": [
        {
          "osmId": "8165171R",
          "name": "Ngaanyatjarra Indigenous Protected Area"
        }
      ],
      "children": 68
    }
  ]
]
```


#### GET /name

Given a latitude / longitude point infer a human readable name for that point.

| Parameter | Description |
| ------------- | ------------- |
| lat           | Latitude of point |
| lon           | Longitude of point |



#### GET /show

Return the details the single node at the end of the given chain of areas.

| Parameter | Description |
| ------------- | ------------- |
| q           | The comma separated list of node ids describing a path down through the area graph. ie. 1,34,899 |


#### GET /points

Returns the outline points for area of the node at the end of the given chain.

| Parameter | Description |
| ------------- | ------------- |
| q           | The comma separated list of node ids describing a path down through the area graph. ie. 1,34,899 |


#### GET /tags

Returns the OSM tags for an OSM id associated with an area from the graph.

| Parameter | Description |
| ------------- | ------------- |
| osm_id        | An OSM id. ie. 123R |



#### Execution

A 2023 full planet graph with 15 million areas fits in a 64Gb heap.

```
sbt -mem 54000 run
```

