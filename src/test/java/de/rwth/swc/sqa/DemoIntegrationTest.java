package de.rwth.swc.sqa;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public class DemoIntegrationTest {
    private static final String PATH = "/customers/1/discountcards";

    @LocalServerPort
    private int port;
    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    public void whenReadAll_thenStatusIsNotImplemented() {
        given().get(PATH).then().statusCode(501);
    }
}