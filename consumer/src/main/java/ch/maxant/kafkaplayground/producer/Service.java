package ch.maxant.kafkaplayground.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path("/c")
public class Service {

    @Inject
    Consumer consumer;

    @GET
    @Path("stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfo(){
        return Response.ok("{\"numMsgs\": " + consumer.getStats() + "}").build();
    }

}