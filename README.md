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

This script passes the Zookeeper

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

    # ip address of container
    docker inspect -f {{.NetworkSettings.IPAddress}} kafka_2

    # status of container, eg. "running"
    docker inspect -f {{.State.Status}} kafka_2

    # attach and run interactively
    docker exec -it kafka_2 bash

    # tail log (-f follows, --tail 100 shows just last 100 lines)
    docker logs -f kafka_2

    # in order to start docker containers in the background, and NOT
    # be flooded with their output, do the following which sends
    # stdout and stderr to dev/null, and runs the process in the
    # background. you can still tail logs as shown above.
    docker run maxant/zookeeper >/dev/null 2>&1 &


Docker ports:

`-p 9091:9092` maps port 9092 from the process running inside the Docker container
to port 9091 on the host, so that you can access it from outside via `localhost:9091`.

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

    # subscribe all topics matching regexp
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --whitelist '.*'

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

Toggle load:

    http://localhost:8080/producer/rest/p/toggleLoad

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

- Load Balancing => See notes in `ProducerResource`:
  - https://stackoverflow.com/a/47291442/458370:
    - *"If a valid partition number is specified, that partition will be used when sending the record."*
    - *"If no partition is specified but a key is present a partition will be chosen using a hash of the key."*
    - *"If neither key nor partition is present a partition will be assigned in a round-robin fashion."*
  - The `ProducerResource` sets the key to `null` and doesn't specifiy a parition number, in order to get round-robin load balancing.
- How to seek to the latest record, so skip anything we haven't received since being down? => no need to worry about that since the cluster knows whichi offset the group last processed
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
- If the number of consumers is changed, Kafka rebalances. When there is no load, rebalancing takes between 5 and 20 ms.
  Under a load of producing more than 300 records per second (and consumed by two consumers, limited by creation rather
  than consumption; running on a single i5 Thinkpad laptop at roughly 100% CPU), one of the two consumers was shut down.
  The remaining consumer logged the following:

      20:17:58,008 INFO  [stdout] polling...
      20:17:58,010 INFO  [o.a.k.c.c.i.AbstractCoordinator] Attempt to heartbeat failed since group is rebalancing
      20:17:58,015 INFO  [stdout] polled. got 1 records.
      20:17:58,015 INFO  [stdout] ...value = {"createdAt": 1551640678001}
      20:17:58,027 INFO  [stdout] polling...
      20:17:58,029 INFO  [o.a.k.c.c.i.ConsumerCoordinator] Revoking previously assigned partitions [my-topic-2, my-topic-3]
      20:17:58,030 INFO  [o.a.k.c.c.i.AbstractCoordinator] (Re-)joining group
      20:17:58,037 INFO  [o.a.k.c.c.i.AbstractCoordinator] Successfully joined group with generation 13
      20:17:58,038 INFO  [o.a.k.c.c.i.ConsumerCoordinator] Setting newly assigned partitions [another-topic-0, my-topic-0, my-topic-1, my-topic-2, my-topic-3]
      20:17:58,040 INFO  [stdout] polled. got 2 records.
      20:17:58,040 INFO  [stdout] ...value = {"createdAt": 1551640678010}   --> this message was received 30ms after creation in the producer
      20:17:58,045 INFO  [stdout] polling...
      20:17:58,058 INFO  [stdout] polled. got 62 records.
      ...
      20:17:58,092 INFO  [stdout] polled. got 5 records.
      ...
      20:17:58,112 INFO  [stdout] polled. got 13 records.
      ...
      20:17:58,124 INFO  [stdout] polled. got 8 records.
      ...
      20:17:58,131 INFO  [stdout] polled. got 3 records.
      ...
      20:17:58,141 INFO  [stdout] polled. got 2 records.
      ...
      20:17:58,146 INFO  [stdout] polled. got 2 records.
      ...
      20:17:58,152 INFO  [stdout] polled. got 2 records.

  - Under load it was receiving records 14ms after they were created in the producer
  - Before the rebalance, the consumer was receiving one record at a time. Afterwards, it took a few polls to
    stabilise, but then it received 2 records per poll.
  - Rebalancing took about 30ms.
  - The messages received after the rebalance were at most 30ms old (measured as the creation time from the producer)
  - [Chapter 4 of "Kafka: The Definitive Guide"](https://www.oreilly.com/library/view/kafka-the-definitive/9781491936153/ch04.html)
    states: "*During a rebalance, consumers can’t consume messages, so a rebalance is basically a short window of unavailability of the entire consumer group.*",
    but these tests suggest it isn't critical, even under load.



# TODO

_Nothing right now_

