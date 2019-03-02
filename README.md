# Introduction

A place to play with Apache Kafka


# Installation

    wget https://www-eu.apache.org/dist/kafka/2.1.1/kafka_2.11-2.1.1.tgz
    tar -xzf kafka_2.11-2.1.1.tgz
    echo kafka_2.11-2.1.1 >> .gitignore
    rm kafka_2.11-2.1.1.tgz
    git init

# Starting Kafka with docker

The generated `maxant/kafka` image uses a shell script to append important properties to the
`config/server.properties` file in the container, so that it works.
See `start-kafka.sh` for details, including how it MUST set
`advertised.listeners` to the containers IP address, otherwise kafka has
REAL PROBLEMS.

Run `./build.sh` which builds images for Zookeeper and Kafka

Prerequisites: `sudo chmod a+w /etc/hosts`

Run `./run.sh` which starts Zookeeper and two Kafka brokers with IDs 1 and 2,
listening on ports 9091 and 9092 respectively.

This script also shows how to append the hosts file so that service names can
be used by applications, but it requires the user to `sudo`. It also contains
examples of waiting for Zookeeper / Kafka logs to contain certain logs before
the script continues.

Useful Docker commands:

    docker exec -it kafka_1 bash

    # stopped / killed processes
    docker ps --all

    # remove container (so new one can be run)
    docker rm kafka_2

    # kill container
    docker kill kafka_2

    # delete images
    docker rmi maxant/kafka

    # or you can use just first few letters of id
    docker rmi 23 48 35 3c3 0

Docker ports:

# Starting Kafka locally

    # adjust memory if required
    export KAFKA_HEAP_OPTS="-Xmx500M -Xms500M"

    # start zookeeper
    cd kafka_2.11-2.1.1
    bin/zookeeper-server-start.sh config/zookeeper.properties

    # create two configs to set up multi-broker 
    cp config/server.properties config/server1.properties 
    cp config/server.properties config/server2.properties 

    # modify both file respectively (use ports 9091 & 9092):
    #     broker.id=2
    #     listeners=PLAINTEXT://:9092
    #     log.dir=/tmp/kafka-logs-2

    # start kafka
    bin/kafka-server-start.sh config/server1.properties
    bin/kafka-server-start.sh config/server2.properties

    # stop kafka
    bin/kafka-server-stop.sh config/server1.properties
    bin/kafka-server-stop.sh config/server2.properties

    # create topic
    # two partition replicated to both nodes. otherwise theres no load balancing.
    bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 2 --topic my-topic

    # list topics:
    bin/kafka-topics.sh --list --zookeeper localhost:2181

    # describe topics
    bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic my-topic

    # alter topic
    bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic my-topic --partitions 2

    # publish:
    bin/kafka-console-producer.sh --broker-list localhost:9092 --topic my-topic

    # subscribe:
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic my-topic --from-beginning

    # list brokers:
    bin/zookeeper-shell.sh localhost:2181 <<< "ls /brokers/ids"


# Starting the Producer

    cd producer

    mvn clean install -DskipTests && java -jar target/producer-swarm.jar

or...

    mvn clean package && java -jar target/producer-swarm.jar

Publish a message asynchronously:

    http://localhost:8080/producer/rest/p/async

or synchronously...

    http://localhost:8080/producer/rest/p/sync

Override the bootstrap list like this:

    -Dkafka.bootstrap.servers=localhost:9092,localhost:9091

# Starting the Consumer

    cd consumer

    mvn clean install -DskipTests && java -jar target/consumer-swarm.jar

or...

    mvn clean package && java -jar target/consumer-swarm.jar

Start a second consumer to test load balancing:

    java -Dswarm.port.offset=2 -jar target/consumer-swarm.jar

Override the bootstrap list like this:

    -Dkafka.bootstrap.servers=localhost:9092,localhost:9091

# Windows

    bin\windows\zookeeper-server-start.bat config\zookeeper.properties
    bin\windows\kafka-server-start.bat config\server1.properties
    bin\windows\kafka-server-start.bat config\server2.properties
    bin\windows\kafka-topics.bat --describe --zookeeper localhost:2181 --topic my-topic

    C:\ieu\devspace\workspaces\trunk\kafkaplayground
    C:\ieu\apache-maven-3.3.9\bin\mvn --settings C:\ieu\devspace\devspace_3.1\eclipse\configuration\.m2\settings.xml clean install


# Notes

- Load Balancing => See notes in ProducerResource
- How to seek to latest, so skip anything we havent received since being down? => no need to worry about that since the cluster knows whichi offset the group last processed
- how to simulate jms topic? => send UUID with to groupId
    - but that needs us to be able to tell it from which offset to start. ie to skip everything, ie a non-durable topic consumer => consumer.seek(...).
- if a client's offset is stored in zookeeper and client then fails catastrophically and never restarts, will that work? since cliend ID is a UUID, no other client will every take over from that last offset. => yes, since the offset is stored per group-id
- is it possible to create topics on the fly or programatically or via REST ie without admin command line?
    - yes, but then theres no control over partitioning etc.
- if a consumer runs before topic is created, then topic is created with default values.
- to reset everything, stop consumers, stop producers, stop nodes, stop zookeeper, delete everything under /tmp/, restart zookeeper, restart nodes, restart consumers, restart producers
- if you have more consumers than partitions, they don't get any messages. you can have maximum one
  consumer per partition (within a consumer group). e.g.:
  - 1 consumer,  4 partitions => all records go to single consumer
  - 2 consumers, 4 partitions => two each
  - 3 consumers, 4 partitions => one gets two partition, the other two get one each
  - 4 consumers, 4 partitions => one each
  - 5 consumers, 4 partitions => **one idle**, 4 get one partition each

# TODO

_Nothing right now_

