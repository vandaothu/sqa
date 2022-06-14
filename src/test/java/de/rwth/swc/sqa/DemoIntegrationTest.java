package de.rwth.swc.sqa;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.threeten.extra.MutableClock;

import java.time.Clock;
import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = MutableClockConfig.class, webEnvironment = RANDOM_PORT)
public class DemoIntegrationTest {
    private static final String PATH = "/customers/1/discountcards";

    @LocalServerPort
    private int port;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    public void whenReadAll_thenStatusIsNotImplemented() {
        clock.setInstant(Instant.parse("2022-01-01T00:00:00.00Z"));
        given().get(PATH).then().statusCode(501);
    }
}