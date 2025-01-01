input="new-zealand-241231"

# exit when any command fails
set -e

mkdir -p $input

jarfile="osm-jigsaw-parser-assembly-1.0.jar"
java -jar $jarfile -s boundaries $input
java -Xmx16G -jar $jarfile -s extract $input
java -Xmx16G -jar $jarfile -s areaways $input
java -Xmx16G -jar $jarfile -s areas $input

time java -Xmx12G -jar $jarfile -s graph $input

java -jar $jarfile -s tags $input
java -jar $jarfile -s flip $input

#time java -Xmx12G -jar $jarfile -s tile $input
