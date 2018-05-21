package ch.maxant.kafkaplayground.producer;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.undertow.util.StatusCodes;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

public class ConsumerIT {

    @Test
    public void test() throws Exception {

        RequestSpecBuilder builder = new RequestSpecBuilder();
        builder.setBaseUri(getBaseUriForLocalhost());
        builder.setAccept(ContentType.JSON);
        RequestSpecification spec = builder.build();

        given(spec)
                .when()
                .get("/consumer/rest/c")
                .then()
                .log().body()
                .statusCode(StatusCodes.OK)
                .body("name", is("fdsa"));
    }

    public String getBaseUriForLocalhost() {
        return "http://localhost:" + (8080 + Integer.getInteger("swarm.port.offset", 1));
    }
}
