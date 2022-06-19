package de.rwth.swc.sqa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
public class CustomerTest {

    @Autowired
    private ObjectMapper objectMapper;
    private static final String PATH = "/customers";

    static List<String> customerPara = new ArrayList<>();

    @LocalServerPort
    private static int port = 8080;

    @BeforeAll
    public static void setUp() {

        RestAssured.port = port;
        customerPara = List.of("id", "birthdate", "disabled");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2023-10-20","1999-13-20","abc-10-20","1999-10-32","1999-10-20","-1"})
    public void addCustomerDateTest(String date) {
        boolean errorInDate = false;
        boolean jsonError = false;

        try {
            LocalDate birthdate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            if(birthdate.isAfter(LocalDate.now())) {
                errorInDate = true;
            }
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
            //id doesn't contain non-number character
            assertThat(response.then().extract().path("id").toString().matches("[^0-9]")).isFalse();

            //get keys from response body
            JSONObject myObject = null;
            try {
                myObject = new JSONObject(response.getBody().asString());
            } catch (JSONException e) {
                jsonError=true;
            }
            List<String> keys = new ArrayList<>();
            myObject.keys().forEachRemaining(key -> keys.add(key.toString()));

            //assure that returned response has the defined schema
            customerPara.forEach(para -> {
                Assertions.assertThat(keys.contains(para)).isTrue();
            });

        }
        Assertions.assertThat(jsonError).isFalse();
    }


    @Test
    public void addDisabledCustomerTest() {
        boolean jsonError=false;
        //boolean value as disabled
        ObjectNode postbody = objectMapper.createObjectNode().put("birthdate","1999-10-11").put("disabled",true);
        Response response = given().contentType("application/json").body(postbody.toString()).post(PATH);
        response.then().assertThat().
                statusCode(201).
                body("birthdate", equalTo("1999-10-11")).
                body("disabled",equalTo(true));

        //id doesn't contain non-number character
        assertThat(response.then().extract().path("id").toString().matches("[^0-9]")).isFalse();


        //get keys from response body
        JSONObject myObject = null;
        try {
            myObject = new JSONObject(response.getBody().asString());
        } catch (JSONException e) {
            jsonError=true;
        }
        List<String> keys = new ArrayList<>();
        myObject.keys().forEachRemaining(key -> keys.add(key.toString()));

        //assure that returned response has the defined schema
        customerPara.forEach(para -> {
            Assertions.assertThat(keys.contains(para)).isTrue();
        });

        //non-boolean value as disabled
        ObjectNode errorpostbody = objectMapper.createObjectNode().put("birthdate","1999-10-11").put("disabled","ja");
        Response errorresponse = given().contentType("application/json").body(errorpostbody.toString()).post(PATH);
        errorresponse.then().assertThat().statusCode(400);

        Assertions.assertThat(jsonError).isFalse();
    }

    @Test
    public void malformedElementJsonRequestTest(){
        ObjectNode customerRequest = objectMapper.createObjectNode();

        //test missing element
        given().log().all().contentType("application/json").body(customerRequest.toString())
                .post(PATH).then().assertThat().statusCode(400);

        //test malformed in type
       customerRequest.put("birthdate",true);
        given().contentType("application/json").body(customerRequest.toString())
                .post(PATH).then().assertThat().statusCode(400);

        //redundant element
        customerRequest.put("redundant_element",true);
        given().contentType("application/json").body(customerRequest.toString())
                .post(PATH).then().assertThat().statusCode(400);
    }





}