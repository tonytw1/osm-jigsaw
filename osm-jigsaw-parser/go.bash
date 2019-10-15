input="great-britain-190827"
jarfile="osm-jigsaw-parser-assembly-1.0.jar"

#time java -jar $jarfile -s boundaries $input
java -Xmx8G -jar $jarfile -s extract $input
#java -Xmx8G -jar $jarfile -s areaways $input
#java -Xmx8G -jar $jarfile -s areas $input
#time java -Xmx8G -jar $jarfile -s graph $input.areas.pbf $input.graph.pbf

#java -jar $jarfile -s tags $input.rels.pbf $input.areas.pbf $input.tags.pbamf
