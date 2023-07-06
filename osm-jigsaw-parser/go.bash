# exit when any command fails
set -e

#input="great-britain-230704"
input="ireland-and-northern-ireland-180717"
input="malta-230704"

jarfile="osm-jigsaw-parser-assembly-1.0.jar"

#java -jar $jarfile -s boundaries $input
#java -Xmx8G -jar $jarfile -s extract $input
#java -Xmx8G -jar $jarfile -s areaways $input
#java -Xmx8G -jar $jarfile -s areas $input
time java -Xmx8G -jar $jarfile -s graph $input
#java -jar $jarfile -s tags $input 
