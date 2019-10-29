# exit when any command fails
set -e

input="great-britain-191028"
jarfile="osm-jigsaw-parser-assembly-1.0.jar"

java -jar $jarfile -s boundaries $input
java -Xmx8G -jar $jarfile -s extract $input
java -Xmx8G -jar $jarfile -s areaways $input
java -Xmx8G -jar $jarfile -s areas $input
java -Xmx8G -jar $jarfile -s graph $input.areas.pbf $input.graph.pbf
java -jar $jarfile -s tags $input $input.tags.pbf
