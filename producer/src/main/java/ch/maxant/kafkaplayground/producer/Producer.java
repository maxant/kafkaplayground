package ch.maxant.kafkaplayground.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path("/p")
public class Producer {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInfo(){
        Model model = new Model();
        model.setName("asdf");
        return Response.ok(model).build();
    }

}