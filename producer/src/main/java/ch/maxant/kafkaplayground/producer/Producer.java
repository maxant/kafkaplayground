package ch.maxant.kafkaplayground.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
@Path("/p")
public class Producer {

    private static AtomicLong totalWait = new AtomicLong();
    private static AtomicLong totalNumCalls = new AtomicLong();

    private ObjectMapper objectMapper;
    private org.apache.kafka.clients.producer.Producer<String, String> producer;

    @PostConstruct
    public void init(){
        objectMapper = new ObjectMapper();
        producer = createKafkaPublisher();
    }

    @PreDestroy
    public void close(){
        producer.close();
    }

    @GET
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sync() throws Exception {

        Model model = processRequest();

        ProducerRecord<String, String> record = createRecord(model);

        // with this approach we block the thread until we get confirmation that the message has been published
        // according to our requirements (acks=3), but at the same time we don't commit the local transaction
        // until we are sure the message has been received by kafka => guaranteed delivery at the expense of
        // threads and performance in the producer.
        long now = System.currentTimeMillis();
        Future<RecordMetadata> response = producer.send(record);
        System.out.println("Sent message to Kafka...");
        RecordMetadata metadata = response.get(30, TimeUnit.SECONDS); //blocks!
        updateStats(now);
        System.out.println("Got ACK for message " + model.getId() + " " + metadata);
        System.out.println("Average ACK " + ((double) totalWait.get() / totalNumCalls.get()) + "ms");

        // no need to update our model, at this stage we know for sure that it has been received by kafka,
        // at the expense of blocking the thread

        return Response.ok(model).build();
    }

    private void updateStats(long now) {
        long diff = System.currentTimeMillis() - now;
        if(diff < 100) { // ignore large values eg at startup
            totalWait.addAndGet(diff);
            totalNumCalls.incrementAndGet();
        }
    }

    @GET
    @Path("async")
    @Produces(MediaType.APPLICATION_JSON)
    public Response async() throws Exception {

        Model model = processRequest();

        ProducerRecord<String, String> record = createRecord(model);

        // here a different approach is used with a callback which then uses a new local transaction to
        // commit an update to the database. so on the one hand we'd have no blocked thread, on the other
        // a second transaction with an update statement.  its a trade off - I guess it would need to be
        // performance tested to determine the best approach. a library encapsulating this functionality
        // should probably offer both options to the application developer.

        long now = System.currentTimeMillis();
        producer.send(record, (metadata, exception) -> { //non-blocking!
            updateStats(now);
            System.out.println("Got ACK for message " + model.getId() + " " + metadata);
            System.out.println("Average ACK " + ((double) totalWait.get() / totalNumCalls.get()) + "ms");

            // TODO here we'd call an bean which starts a new transaction in order to update
            // the state from pending to sent, so that we don't retry at a later time...
        });
        System.out.println("Sent message to Kafka...");

        return Response.ok(model).build();
    }

    private ProducerRecord<String, String> createRecord(Model model) throws JsonProcessingException {
TODO split framework and programmer code
TODO get topic from props and fail if not present

        String key = "model-created";
        return new ProducerRecord<>(
                topic,
                key,
                objectMapper.writeValueAsString(model));
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

    private Model processRequest() {
        Model model = new Model();
        model.setId(UUID.randomUUID().toString());
        model.setName("asdf");
        return model;
    }

}