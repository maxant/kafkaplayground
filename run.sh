#########################################################
# runs images. ensure you call build.sh first!
#########################################################

#######################################
# start zookeeper

echo killing zookeeper...
docker kill zookeeper
echo removing zookeeper container...
docker rm zookeeper
docker run --name zookeeper -p 2181:2181 maxant/zookeeper &

sleep 2.0;

#until [ "`docker inspect -f {{.State.Status}} zookeeper`"=="running" ]; do
#    echo waiting for zookeeper status
#    sleep 0.1;
#done;
zookeeperhost=$(docker inspect -f {{.NetworkSettings.IPAddress}} zookeeper)
echo zookeeper address = $zookeeperhost
#echo waiting for zookeeper to start...
#until [ "`docker inspect -f {{.State.Running}} zookeeper`"=="true" ]; do
#until [ "`echo stat | nc $zookeeperhost 2181 | grep Connections`"=="Connections: 1" ]; do
#until [ "`echo stat | nc 172.17.0.2 2181 | grep Connections`"=="Connections: 1" ]; do
#    echo waiting for zookeeper to start...
#    sleep 1.0;
#done;
echo Zookeeper started:
echo stat | nc $zookeeperhost 2181 | grep Zookeeper

#######################################
# get ip address from docker0 interface and grep just the ip address
dockerhost=$(ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+')
echo docker host is: $dockerhost

#######################################
# similar to above; head takes just first result, as inspect contains two such lines
#zookeeperhost=$(docker inspect zookeeper | grep -Po 'IPAddress": "\K[\d.]+' | head -n1)
#echo zookeeper: $zookeeperhost

#######################################
# start kafka brokers

for id in {1..2}
do
    p=909$id:9092
    docker kill kafka_$id
    docker rm kafka_$id
    docker run --name kafka_$id -p $p -e "ID=$id" -e "DOCKER_HOST=$dockerhost" -e "ZOOKEEPER_HOST=$zookeeperhost" maxant/kafka &
done

