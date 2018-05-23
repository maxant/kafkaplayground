package ch.maxant.kafkaplayground.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@ApplicationScoped
public class ProducerService {

    private static AtomicLong totalWait = new AtomicLong();
    private static AtomicLong totalNumCalls = new AtomicLong();

    private org.apache.kafka.clients.producer.Producer<String, String> producer;

    @PostConstruct
    public void init(){
        producer = createKafkaPublisher();
    }

    @PreDestroy
    public void close(){
        producer.close();
    }

    public void sync(String topic, String key, String value) throws Exception {

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

        // with this approach we block the thread until we get confirmation that the message has been published
        // according to our requirements (acks=3), but at the same time we don't commit the local transaction
        // until we are sure the message has been received by kafka => guaranteed delivery at the expense of
        // threads and performance in the producer.
        long now = System.currentTimeMillis();
        Future<RecordMetadata> response = producer.send(record);
        System.out.println("Sent message to Kafka...");
        RecordMetadata metadata = response.get(30, TimeUnit.SECONDS); //blocks!
        updateStats(now);
        System.out.println("Got ACK for message " + value + " " + metadata);
        System.out.println("Average ACK " + ((double) totalWait.get() / totalNumCalls.get()) + "ms");

        // no need to update our model, at this stage we know for sure that it has been received by kafka,
        // at the expense of blocking the thread
    }

    public void async(String topic, String key, String value, Consumer f) throws Exception {

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

        // here a different approach is used with a callback which then uses a new local transaction to
        // commit an update to the database. so on the one hand we'd have no blocked thread, on the other
        // a second transaction with an update statement.  its a trade off - I guess it would need to be
        // performance tested to determine the best approach. a library encapsulating this functionality
        // should probably offer both options to the application developer.

        long now = System.currentTimeMillis();
        producer.send(record, (metadata, exception) -> { //non-blocking!
            if(exception != null){
                // TODO handle exception
                exception.printStackTrace();
            }else{
                updateStats(now);
                System.out.println("Got ACK for message " + value + " " + metadata);
                System.out.println("Average ACK " + ((double) totalWait.get() / totalNumCalls.get()) + "ms");

                f.accept(null);
            }
        });
        System.out.println("Sent message to Kafka...");
    }

    private void updateStats(long now) {
        long diff = System.currentTimeMillis() - now;
        if(diff < 100) { // ignore large values eg at startup
            totalWait.addAndGet(diff);
            totalNumCalls.incrementAndGet();
        }
    }

    private org.apache.kafka.clients.producer.Producer<String, String> createKafkaPublisher() {
        Properties props = new Properties();
        props.put("bootstrap.servers", System.getProperty("kafka.bootstrap.servers", "localhost:9093"));
        props.put("acks", System.getProperty("kafka.acks", "all")); //ensures all nodes have it rather than just the leader, so that we can sleep better knowing that a kafka failure wont lead to lost events
        props.put("retries", Integer.parseInt(System.getProperty("kafka.retries", "0"))); //leave this at 0 - producer javadocs talks about danger of dups otherwise
        props.put("batch.size", Integer.parseInt(System.getProperty("kafka.batch.size", "16384")));
        props.put("key.serializer", StringSerializer.class.getCanonicalName());
        props.put("value.serializer", StringSerializer.class.getCanonicalName());

        return new KafkaProducer<>(props);
    }

}