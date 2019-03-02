#########################################################
# runs images. ensure you call build.sh first!
#########################################################

sudo echo ""

#########################################################
# stop kafka brokers
for id in {1..1}
do
    p=909$id:9092

    if [ `docker ps --all | grep kafka_$id | wc -l` -ge 1 ]
      then
        if [[ "`docker inspect -f {{.State.Status}} kafka_$id`" == "running" ]]
          then
            echo killing kafka_$id...
            docker kill kafka_$id
        else
            echo kafka_$id not running
        fi
        echo removing kafka_$id container...
        docker rm kafka_$id
    else
        echo kafka_$id not found
    fi

done


#########################################################
# zookeeper

if [ `docker ps --all | grep zookeeper | wc -l` -ge 1 ]
  then
    if [[ "`docker inspect -f {{.State.Status}} zookeeper`" == "running" ]]
      then
        echo killing zookeeper...
        docker kill zookeeper
    else
        echo zookeeper not running
    fi
    echo removing zookeeper container...
    docker rm zookeeper
else
    echo zookeeper not found
fi

echo starting zookeeper...
docker run --name zookeeper -p 2181:2181 maxant/zookeeper >/dev/null 2>&1 &

#"INFO binding to port 0.0.0.0/0.0.0.0:2181"
counter=0
until [ $counter -ge 1 ]
do
    echo waiting for zookeeper...
    sleep 0.5;
    counter=`docker logs zookeeper | grep binding | wc -l`
done

zookeeperhost=$(docker inspect -f {{.NetworkSettings.IPAddress}} zookeeper)
echo zookeeper is up: $zookeeperhost

hosts=/etc/hosts
if [ -n "$(grep zookeeper $hosts)" ]
    then
        echo "zookeeper found in hosts file, removing now...";
        sudo sed -i '/.*zookeeper.*/d' $hosts
    else
        echo "zookeeper was not found in $hosts";
    fi
echo appending zookeeper to $hosts
sudo echo "$zookeeperhost zookeeper" >> $hosts
cat $hosts

#########################################################
# get ip address from docker0 interface and grep just the ip address
dockerhost=$(ip -4 addr show docker0 | grep -Po 'inet \K[\d.]+')
echo docker host is: $dockerhost

#########################################################
# start kafka brokers

kafkahosts=
for id in {1..1}
do
    p=909$id:9092

    echo starting kafka_$id...

    docker run --name kafka_$id -p $p -e "ID=$id" -e "DOCKER_HOST=$dockerhost" -e "ZOOKEEPER_HOST=$zookeeperhost" maxant/kafka >/dev/null 2>&1 &

    sleep 0.5;

    kafkahost=$(docker inspect -f {{.NetworkSettings.IPAddress}} kafka_$id)
    echo kafka_$id is up: $kafkahost
    kafkahosts=$kafkahost,$kafkahosts
done

sleep 2
echo COMPLETED. Kafka boostrap servers: $kafkahosts