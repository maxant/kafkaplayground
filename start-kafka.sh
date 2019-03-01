#########################################################
# appends properties to end of config file
# and then starts kafka as normal
#########################################################

file=/kafka/config/server.properties

#new lines at end of file
echo  "" >>                 $file
echo  "" >>                 $file
echo  "# MAXANT CONFIG:" >> $file

echo zookeeper.connect=$ZOOKEEPER_HOST:2181 >> $file
echo broker.id=$ID >>                          $file

# if you need to use the docker host, you could do it like this:
#echo zookeeper.connect=$DOCKER_HOST:2181 >>    $file

cat /kafka/config/server.properties

./bin/kafka-server-start.sh $file
