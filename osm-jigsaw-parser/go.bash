input="great-britain-180719"
jarfile="osm-jigsaw-assembly-0.1.0-SNAPSHOT.jar"
#java -jar $jarfile -s split $input.osm.pbf
#java -Xmx16G -jar $jarfile -s extract $input.osm.pbf $input.rels.pbf
#java -Xmx16G -jar $jarfile -s areaways $input.rels.pbf $input.areaways.pbf
java -Xmx16G -jar $jarfile -s areas $input.rels.pbf $input.areaways.pbf $input.areas.pbf
#java -jar $jarfile -s tags $input.rels.pbf $input.areas.pbf $input.tags.pbf
#time java -Xmx16G -jar $jarfile -s graph $input.areas.pbf $input.graph.pbf
