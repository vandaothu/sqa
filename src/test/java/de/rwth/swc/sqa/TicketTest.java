package de.rwth.swc.sqa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONException;
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
public class TicketTest {
    @Autowired
    private ObjectMapper objectMapper;
    private static final String PATH = "/tickets";

    @LocalServerPort
    private int port = 8080;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    // test correct birthdate input
    @ParameterizedTest
    @ValueSource(strings = {"2023-10-20","1999-13-20","abc-10-20","1999-10-32","1999-10-20"})
    public void buyTicketDateTest(String date) throws JSONException {
        boolean errorInBirthDate = false;

        try {
            LocalDate birthdate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        catch (DateTimeParseException e) {
            errorInBirthDate = true;
        }

        ObjectNode postbody = objectMapper.createObjectNode().put("birthdate",date)
                                                            .put("validFrom", "2022-05-26T13:00:00")
                                                            .put("validFor", "30d");
        Response response = given().contentType("application/json").body(postbody.toString()).post(PATH);
        if(errorInBirthDate){
            response.then().statusCode(400);
        }
        else {
            response.then().assertThat()
                    .body("validFrom", equalTo("2022-05-26T13:00:00"))
                    .body("birthdate", equalTo(date))
                    .body("LDBirthDate", equalTo(date))
                    .body("validFor", equalTo("30d"))
                    .body("disabled", equalTo(false))
                    .body("discountCard", equalTo(false))
                    .body("zone", equalTo(null))
                    .body("student",equalTo(false))
                    .body("from", equalTo("2022-05-26T13:00:00"))
                    .body("until", equalTo("2022-06-25T13:00:00"))
                    .statusCode(201);
            assertThat(response.then().extract().path("id").toString().matches("[0-9]")).isTrue();
        }
    }

    // test correct validFrom input 
    @ParameterizedTest
    @ValueSource(strings = {"xxxx-05-26T13:00:00","2022-13-26T26:00:00","2022-05-26T13:xyz:00","2022-05-26T13:00:61","2022-05-26T13:00:00"})
    public void buyTicketValidFromTest(String datetime) throws JSONException {
        boolean errorInValidFrom = false;

        try {
            LocalDate valid = LocalDate.parse(datetime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        catch (DateTimeParseException e) {
            errorInValidFrom = true;
        }

        ObjectNode postbody = objectMapper.createObjectNode().put("birthdate","1992-05-26")
                                                            .put("validFrom", datetime)
                                                            .put("validFor", "1y");
        Response response = given().contentType("application/json").body(postbody.toString()).post(PATH);
        if(errorInValidFrom){
            response.then().statusCode(400);
        }
        else {
            response.then().assertThat()
                    .body("validFrom", equalTo("2022-05-26T13:00:00"))
                    .body("birthdate", equalTo("1992-05-26"))
                    .body("LDBirthDate", equalTo("1992-05-26"))
                    .body("validFor", equalTo("1y"))
                    .body("disabled", equalTo(false))
                    .body("discountCard", equalTo(false))
                    .body("zone", equalTo(null))
                    .body("student",equalTo(false))
                    .body("from", equalTo(datetime))
                    .body("until", equalTo("2023-05-26T13:00:00"))
                    .statusCode(201);
            assertThat(response.then().extract().path("id").toString().matches("[0-9]")).isTrue();
        }
    }


    @Test
    public void buyTicketDisableTest() {
        
        //set disable to true
        ObjectNode body = objectMapper.createObjectNode().put("validFrom","2022-05-26T13:00:00")
                                                            .put("birthdate","2012-12-13")
                                                            .put("validFor","30d")
                                                            .put("disabled",true);                    
        Response response = given().contentType("application/json").body(body.toString()).post(PATH);
        response.then().assertThat()
                .statusCode(201)
                .body("validFrom", equalTo("2022-05-26T13:00:00"))
                .body("birthdate", equalTo("2012-12-13"))
                .body("LDBirthDate", equalTo("2012-12-13"))
                .body("validFor", equalTo("30d"))
                .body("disabled", equalTo(true))
                .body("discountCard", equalTo(false))
                .body("zone", equalTo(null))
                .body("student",equalTo(false))
                .body("from", equalTo("2022-05-26T13:00:00"))
                .body("until", equalTo("2022-06-25T13:00:00"));
        assertThat(response.then().extract().path("id").toString().matches("[0-9]")).isTrue();

        ObjectNode errorBody = objectMapper.createObjectNode().put("validFrom","2022-05-26T13:00:00")
                                                            .put("birthdate","2012-12-13")
                                                            .put("validFor","30d")
                                                            .put("disabled","ja");                             
        Response errorResponse = given().contentType("application/json").body(errorBody.toString()).post(PATH);
        errorResponse.then().assertThat().statusCode(400);
    }

    @Test
    public void buyTicketStudentTest() {
        
        //set student to true
        ObjectNode body = objectMapper.createObjectNode().put("validFrom","2022-05-26T13:00:00")
                                                            .put("birthdate","2002-12-13")
                                                            .put("validFor","30d")
                                                            .put("student",true);                               
        Response response = given().contentType("application/json").body(body.toString()).post(PATH);
        response.then().assertThat()
                .statusCode(201)
                .body("validFrom", equalTo("2022-05-26T13:00:00"))
                .body("birthdate", equalTo("2002-12-13"))
                .body("LDBirthDate", equalTo("2002-12-13"))
                .body("validFor", equalTo("30d"))
                .body("disabled", equalTo(false))
                .body("discountCard", equalTo(false))
                .body("zone", equalTo(null))
                .body("student",equalTo(true))
                .body("from", equalTo("2022-05-26T13:00:00"))
                .body("until", equalTo("2022-06-25T13:00:00"));
        assertThat(response.then().extract().path("id").toString().matches("[0-9]")).isTrue();

        ObjectNode errorBody = objectMapper.createObjectNode().put("validFrom","2022-05-26T13:00:00")
                                                            .put("birthdate","1992-12-13")
                                                            .put("validFor","1d")
                                                            .put("student",true);                         
        Response errorResponse = given().contentType("application/json").body(errorBody.toString()).post(PATH);
        errorResponse.then().assertThat().statusCode(400);
    }

    @Test
    public void buyTicketDiscountTest() {
        
        //set discount to true
        ObjectNode body = objectMapper.createObjectNode().put("validFrom","2022-05-26T13:00:00")
                                                            .put("birthdate","2002-12-13")
                                                            .put("validFor","30d")
                                                            .put("discountCard", true);                          
        Response response = given().contentType("application/json").body(body.toString()).post(PATH);
        response.then().assertThat()
                .statusCode(201)
                .body("validFrom", equalTo("2022-05-26T13:00:00"))
                .body("birthdate", equalTo("2002-12-13"))
                .body("LDBirthDate", equalTo("2002-12-13"))
                .body("validFor", equalTo("30d"))
                .body("disabled", equalTo(false))
                .body("discountCard", equalTo(true))
                .body("zone", equalTo(null))
                .body("student",equalTo(false))
                .body("from", equalTo("2022-05-26T13:00:00"))
                .body("until", equalTo("2022-06-25T13:00:00"));
        assertThat(response.then().extract().path("id").toString().matches("[0-9]")).isTrue();

        ObjectNode errorBody = objectMapper.createObjectNode().put("validFrom","2022-05-26T13:00:00")
                                                            .put("birthdate","1992-12-13")
                                                            .put("validFor","1d")
                                                            .put("discountCard",true);                             
        Response errorResponse = given().contentType("application/json").body(errorBody.toString()).post(PATH);
        errorResponse.then().assertThat().statusCode(400);
    }
}
