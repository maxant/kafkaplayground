# Introduction

A place to play with Apache Kafka


# Installation

    wget http://www-eu.apache.org/dist/kafka/1.1.0/kafka_2.11-1.1.0
    tar -xzf kafka_2.11-1.1.0.tgz
    echo kafka_2.11-1.1.0.tgz > .gitignore
    rm kafka_2.11-1.1.0.tgz
    git init

# Starting Kafka

    # start zookeeper
    cd kafka_2.11-1.1.0
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
    bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 1 --topic my-topic

    # list topics:
    bin/kafka-topics.sh --list --zookeeper localhost:2181

    # describe topics
    bin/kafka-topics.sh --describe --zookeeper localhost:2181 --topic my-topic

    # publish:
    bin/kafka-console-producer.sh --broker-list localhost:9093 --topic my-topic

    # subscribe:
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9093 --topic my-topic --from-beginning


# TODO

- is it possible to create topics on the fly or programatically or via REST ie without admin command line?
- if a client's offset is stored in zookeeper and it does, will that work? since cliend ID is a UUID, no other client will every take over from that last offset

