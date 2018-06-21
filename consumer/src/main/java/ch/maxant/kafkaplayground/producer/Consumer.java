package ch.maxant.kafkaplayground.producer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

// just one, since a) we receive batches of records under load so theres less need to spread the load
// and b) the kafka consumer is NOT threadsafe according to javadocs
@Singleton
@Startup
@Lock(LockType.READ) // to avoid timeouts which are not relevant here
public class Consumer {

    public static final String KAFKA_GROUP_ID = "kafka.group.id";
    public static final String KAFKA_TOPICS = "kafka.topics";

    private static boolean commitAsync = Boolean.getBoolean("kafka.commitAsync");

    private static AtomicLong totalWait = new AtomicLong();
    private static AtomicLong totalNumCalls = new AtomicLong();

    @Resource
    SessionContext context;

    private KafkaConsumer<String, String> consumer;
    private AtomicIntegerArray stateOfPlay = new AtomicIntegerArray(new int[]{0,0}); //isPolling; isShuttingDown

    private AtomicLong stats = new AtomicLong();
    private long pollInterval = Integer.getInteger("kafka.poll.interval", 1000);

    @PostConstruct
    public void init() {
        Properties props = getKafkaProperties();
        consumer = new KafkaConsumer<>(props);

        subscribe();

        self().poll(); //do async, so post construct can finish, which allows pre destroy to be called on shutdown. otherwise we cant shutdown gracefully
    }

    private Consumer self() {
        return context.getBusinessObject(Consumer.class);
    }

    @Asynchronous
    public Future<Void> poll() {
        stateOfPlay.set(0, 1); //polling
        if(stateOfPlay.get(1) == 0) { //not shutting down
            try{
                System.out.println("polling...");
                long start = System.currentTimeMillis();
                ConsumerRecords<String, String> records = consumer.poll(pollInterval); //blocks if no data is available
                start = System.currentTimeMillis() - start;
                if(start < pollInterval - 10){
                    totalNumCalls.incrementAndGet();
                    totalWait.addAndGet(start);
                }

                System.out.println("polled. got " + records.count() + " records. avg poll time where data available: " + ((double) totalWait.get() / totalNumCalls.get()) + "ms");

                handleRecords(records);

                commitKafka();

            } catch (KafkaException e) {
                System.err.println("Failed to poll, see stack trace below");
                e.printStackTrace();
            } finally {
                stateOfPlay.set(0, 0); //no longer polling
                self().poll();
            }
        }

        return new AsyncResult<>(null);
    }

    private void handleRecords(ConsumerRecords<String,String> records) {
        //TODO start new TX
        for (ConsumerRecord<String, String> record : records) {
            StringBuilder headers = new StringBuilder();
            record.headers().forEach(h -> headers.append(h).append(", "));
            System.out.printf("offset = %d, timestamp = %d, topic = %s, partition = %d, key = %s, value = %s%n, headers = %s", record.offset(), record.timestamp(), record.topic(), record.partition(), record.key(), record.value(), headers);

            stats.incrementAndGet();

            // TODO store record in inner TX and fire command to handle processing of
            // new record async, so this thread isnt blocked. only store it if the UUID in the payload is
            // not already known => we are idempotent here
        }
    }

    private void commitKafka() {
        // we commit all events together! not possible to commit individual offsets
        if (commitAsync) {
            consumer.commitAsync((offsets, exception) -> {
                if (exception != null) {
                    // TODO handle exception
                    exception.printStackTrace();
                } else {
                    // TODO if we want to store the offset ourselves, then we do that here by calling
                    // a bean which starts a new transaction in order to update
                    // the database
                    offsets.forEach((key, value) -> System.out.printf("committed to topic %s, partition %d, offset %d, metadata %s", key.topic(), key.partition(), value.offset(), value.metadata()));
                }
            });
        } else {
            // blocks. stores offsets in kafka. can lead to duplicates, but that is
            // ok for our use case since we don't allow duplicate events based on UUID in value (payload)
            long start = System.currentTimeMillis();
            consumer.commitSync();
            System.out.println("commit sync in " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    private void subscribe() {
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
    }

    @PreDestroy
    public void close() {
        stateOfPlay.set(1, 1); //shutting down
        System.out.println("closing kafka consumer...");
        while(stateOfPlay.get(0) == 1){
            //still polling, so wait until thats done, then close consumer
           System.out.println("waiting for pending kafka poll request...");
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("polling complete, closing kafka consumer now...");
        consumer.close();
        System.out.println("kafka consumer closed");
    }

    private Properties getKafkaProperties(){
        Properties props = new Properties();
        props.put("bootstrap.servers", System.getProperty("kafka.bootstrap.servers", "localhost:9093"));

        String groupId = System.getProperty(KAFKA_GROUP_ID);
        if(groupId == null) {
            throw new IllegalArgumentException("System property " + KAFKA_GROUP_ID + " must be set");
        }
        props.put("group.id", groupId);
        props.put("enable.auto.commit", System.getProperty("kafka.enable.auto.commit", "false")); //false since our code commits on its own
        //props.put("auto.commit.interval.ms", Integer.parseInt(System.getProperty("kafka.auto.commit.interval.ms", "1000")));
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }


    public long getStats() {
        return stats.get();
    }
}