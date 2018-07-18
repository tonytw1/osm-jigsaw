input="ireland-and-northern-ireland-180717"
jarfile="osm-jigsaw-assembly-0.1.0-SNAPSHOT.jar"
#java -jar $jarfile -s split $input.osm.pbf
java -Xmx16G -jar $jarfile -s extract $input.osm.pbf $input.rels.pbf
#java -Xmx16G -jar $jarfile -s areas $input.rels.pbf $input.areas.ser
#java -jar $jarfile -s tags $input.rels.pbf $input.tags.pbf
#java -Xmx16G -jar $jarfile -s graph $input.areas.pbf $input.graph.pbf
#java -Xmx28G -jar $jarfile -s export $input.graph.ser $input.graph.pbf
