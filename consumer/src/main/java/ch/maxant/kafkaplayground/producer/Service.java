package ch.maxant.kafkaplayground.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path("/c")
public class Service {

    @GET
    @Path("stats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfo(){
        Model model = new Model();
        model.setName("fdsa");
        return Response.ok(model).build();
    }

}