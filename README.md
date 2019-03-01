# Introduction

A place to play with Apache Kafka


# Installation

    wget https://www-eu.apache.org/dist/kafka/2.1.1/kafka_2.11-2.1.1.tgz
    tar -xzf kafka_2.11-2.1.1.tgz
    echo kafka_2.11-2.1.1 >> .gitignore
    rm kafka_2.11-2.1.1.tgz
    git init

# Starting Kafka

    # adjust memory if required
    export KAFKA_HEAP_OPTS="-Xmx500M -Xms500M"

    # start zookeeper
    cd kafka_2.11-2.1.1
    bin/zookeeper-server-start.sh config/zookeeper.properties

    # create two configs to set up multi-broker 
    cp config/server.properties config/server1.properties 
    cp config/server.properties config/server2.properties 

    # modify both file respectively:
    #     broker.id=2
    #     listeners=PLAINTEXT://:9094
    #     log.dir=/tmp/kafka-logs-2

    # start kafka
    bin/kafka-server-start.sh config/server1.properties
    bin/kafka-server-start.sh config/server2.properties

    # stop kafka
    bin/kafka-server-stop.sh config/server1.properties
    bin/kafka-server-stop.sh config/server2.properties

    # create topic
    # one partition replicated to both nodes. alternatively create more partitions and let the producer decide where to put messages
    bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 1 --topic my-topic

    # list topics:
    bin/kafka-topics.sh --list --zookeeper localhost:2181

    # describe topics
    bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic my-topic

    # publish:
    bin/kafka-console-producer.sh --broker-list localhost:9093 --topic my-topic

    # subscribe:
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9093 --topic my-topic --from-beginning


# Starting the Producer

    cd producer

    mvn clean install -DskipTests && java -jar target/producer-swarm.jar

or...

    mvn clean package && java -jar target/producer-swarm.jar

Publish a message asynchronously:

    http://localhost:8080/producer/rest/p/async

or synchronously...

    http://localhost:8080/producer/rest/p/sync

# Starting the Consumer

    cd consumer

    mvn clean install -DskipTests && java -jar target/consumer-swarm.jar

or...

    mvn clean package && java -jar target/consumer-swarm.jar

Start a second consumer to test load balancing:

    java -Dswarm.port.offset=2 -jar target/consumer-swarm.jar


# Notes

- Load Balancing => See notes in ProducerRestResource
- How to seek to latest, so skip anything we havent received since being down? => no need to worry about that since the cluster knows whichi offset the group last processed
- how to simulate jms topic? => send UUID with to groupId
    - but that needs us to be able to tell it from which offset to start. ie to skip everything, ie a non-durable topic consumer => consumer.seek(...).
- if a client's offset is stored in zookeeper and client then fails catastrophically and never restarts, will that work? since cliend ID is a UUID, no other client will every take over from that last offset. => yes, since the offset is stored per group-id
- is it possible to create topics on the fly or programatically or via REST ie without admin command line?
    - yes, but then theres no control over partitioning etc.


# TODO

_Nothing right now_

