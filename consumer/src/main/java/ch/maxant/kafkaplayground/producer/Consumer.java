package ch.maxant.kafkaplayground.producer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

//TODO just one? or create a few? dont forget about batching...
@Singleton
@Startup
@Lock(LockType.READ)
public class Consumer {

    public static final String KAFKA_GROUP_ID = "kafka.group.id";
    public static final String KAFKA_TOPICS = "kafka.topics";

    private KafkaConsumer<String, String> consumer;
    private boolean live = true;
    private boolean readyForClose = false;

    private AtomicLong stats = new AtomicLong();

    @PostConstruct
    public void init() {
        Properties props = getKafkaProperties();
        consumer = new KafkaConsumer<>(props);

        String topics = System.getProperty(KAFKA_TOPICS);
        if(topics == null) {
            throw new IllegalArgumentException("System property " + KAFKA_TOPICS + " must be set");
        }
        StringTokenizer st = new StringTokenizer(topics, ",");
        List<String> tops = new ArrayList<>();
        while(st.hasMoreTokens()){
            tops.add(st.nextToken());
        }
        consumer.subscribe(tops);
        while (live) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());

                stats.incrementAndGet();

                consumer.com

                //TODO how to confirm? => manual commit!
                //TODO local transaction for local data?
                //TODO how to simulate jms topic? => send UUID with to groupId
                //TODO but that needs us to be able to tell it from which offset to start. ie to skip everything, ie a non-durable topic consumer
            }
        }
        readyForClose = true;
    }

    @PreDestroy
    public void close() throws InterruptedException {
        live = false;
        while(!readyForClose){
            Thread.sleep(1000L);
        }
        consumer.close();
    }

    private Properties getKafkaProperties(){
        Properties props = new Properties();
        props.put("bootstrap.servers", System.getProperty("kafka.bootstrap.servers", "localhost:9093"));

        String groupId = System.getProperty(KAFKA_GROUP_ID);
        if(groupId == null) {
            throw new IllegalArgumentException("System property " + KAFKA_GROUP_ID + " must be set");
        }
        props.put("group.id", groupId);
        props.put("enable.auto.commit", System.getProperty("kafka.enable.auto.commit", "true"));
        props.put("auto.commit.interval.ms", Integer.parseInt(System.getProperty("kafka.auto.commit.interval.ms", "1000")));
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }


}