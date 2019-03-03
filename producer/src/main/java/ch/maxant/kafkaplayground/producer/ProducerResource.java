package ch.maxant.kafkaplayground.producer;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

@ApplicationScoped
@Path("/p")
public class ProducerResource {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    ProducerService producerService;

    @Inject
    LoadProducer loadProducer;

    @GET
    @Path("toggleLoad")
    @Produces(MediaType.APPLICATION_JSON)
    public Response toggleLoad() throws Exception {

        loadProducer.toggle();

        return Response.ok().build();
    }

    @GET
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sync() throws Exception {

        Model model = processRequest();

        producerService.sync(getTopic(), getKey(), objectMapper.writeValueAsString(model));

        // no need to update our model, at this stage we know for sure that it has been received by kafka,
        // at the expense of blocking the thread

        return Response.ok(model).build();
    }

    @GET
    @Path("async")
    @Produces(MediaType.APPLICATION_JSON)
    public Response async() throws Exception {

        Model model = processRequest();

        producerService.async(getTopic(), getKey(), objectMapper.writeValueAsString(model), o -> {
            // TODO here we'd call a bean which starts a new transaction in order to update
            // the state from pending to sent, so that we don't retry at a later time...
        });

        return Response.ok(model).build();
    }

    private String getKey() {

        // https://stackoverflow.com/a/47291442/458370
        //
        // Quote:
        // Here, the field that we have to understand from the class is partition.
        // From the docs,
        // If a valid partition number is specified, that partition will be used when sending the record.
        // If no partition is specified but a key is present a partition will be chosen using a hash of the key.
        // If neither key nor partition is present a partition will be assigned in a round-robin fashion.

        // => since no partition, and no key, we get round robin partitioning
        return null;
    }

    private String getTopic() {
        return "my-topic";
    }

    private Model processRequest() {
        Model model = new Model();
        model.setId(UUID.randomUUID().toString());
        model.setName("asdf");
        return model;
    }

}