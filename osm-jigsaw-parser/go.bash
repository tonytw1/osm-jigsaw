# exit when any command fails
set -e

#input="isle-of-man-190913"
#input="planet-190909"
input="great-britain-191001"

jarfile="osm-jigsaw-parser-assembly-1.0.jar"

#java -jar $jarfile -s boundaries $input
#java -Xmx16G -jar $jarfile -s extract $input
#java -Xmx16G -jar $jarfile -s areaways $input
#java -Xmx28G -jar $jarfile -s areas $input
time java -Xmx31G -jar $jarfile -s graph $input.areas.pbf $input.graph.pbf
#java -jar $jarfile -s tags $input $input.tags.pbf
