package de.rwth.swc.sqa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CustomerTest {

    @Autowired
    private ObjectMapper objectMapper;
    private static final String PATH = "/customers";

    @LocalServerPort
    private int port = 8080;
    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023-10-20","1999-13-20","abc-10-20","1999-10-32","1999-10-20"})
    public void addCustomerDateTest(String date) throws JSONException {
        boolean errorInDate = false;

        try {
            LocalDate birthdate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            /**if(birthdate.isAfter(LocalDate.now())) {
                errorInDate = true;
            }*/
        }
        catch (DateTimeParseException e) {
            errorInDate = true;
        }

        ObjectNode postbody = objectMapper.createObjectNode().put("birthdate",date);
        Response response = given().contentType("application/json").body(postbody.toString()).post(PATH);
        if(errorInDate){
            response.then().statusCode(400);
        }
        else {
            response.then().assertThat().
                    body("birthdate", equalTo(date)).
                    body("disabled",equalTo(false)). //false if not set true
                    statusCode(201);
            assertThat(response.then().extract().path("id").toString().matches("[0-9]")).isTrue();

        }
    }


    @Test
    public void addDisabledCustomerTest(){

        //boolean value as disabled
        ObjectNode postbody = objectMapper.createObjectNode().put("birthdate","1999-10-11").put("disabled",true);
        Response response = given().contentType("application/json").body(postbody.toString()).post(PATH);
        response.then().assertThat().
                statusCode(201).
                body("birthdate", equalTo("1999-10-11")).
                body("disabled",equalTo(true));
        assertThat(response.then().extract().path("id").toString().matches("[0-9]")).isTrue();

        //non-boolean value as disabled
        ObjectNode errorpostbody = objectMapper.createObjectNode().put("birthdate","1999-10-11").put("disabled","ja");
        Response errorresponse = given().contentType("application/json").body(errorpostbody.toString()).post(PATH);
        errorresponse.then().assertThat().statusCode(400);
    }


}