FILE=$1
echo $FILE
cd /data

SCRIPT=/opt/docker/bin/osm-jigsaw-parser

time $SCRIPT -s split $input.osm.pbf
time $SCRIPT -s extract $input.osm.pbf $input.rels.pbf
time $SCRIPT -s areaways $input.rels.pbf $input.areaways.pbf
time $SCRIPT -s areas $input.rels.pbf $input.areaways.pbf $input.areas.pbf
time $SCRIPT -s graph $input.areas.pbf $input.graph.pbf
time $SCRIPT -s tags $input.rels.pbf $input.areas.pbf $input.osm.pbf.named-nodes $input.tags.pbf

