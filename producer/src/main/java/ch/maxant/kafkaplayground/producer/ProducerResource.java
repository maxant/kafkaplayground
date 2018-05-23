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

    @GET
    @Path("sync")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sync() throws Exception {

        Model model = processRequest();

        producerService.sync("my-topic", "model-created", objectMapper.writeValueAsString(model));

        // no need to update our model, at this stage we know for sure that it has been received by kafka,
        // at the expense of blocking the thread

        return Response.ok(model).build();
    }

    @GET
    @Path("async")
    @Produces(MediaType.APPLICATION_JSON)
    public Response async() throws Exception {

        Model model = processRequest();

        producerService.async("my-topic", "model-created", objectMapper.writeValueAsString(model), o -> {
            // TODO here we'd call a bean which starts a new transaction in order to update
            // the state from pending to sent, so that we don't retry at a later time...
        });

        return Response.ok(model).build();
    }

    private Model processRequest() {
        Model model = new Model();
        model.setId(UUID.randomUUID().toString());
        model.setName("asdf");
        return model;
    }

}