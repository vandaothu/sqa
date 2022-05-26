package de.rwth.swc.sqa;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
public class DiscountCardTest {

    private final String path(Long customerId){
        return "/customers/"+customerId+"/discountcards";
    }
    private static List<Long> customerIds = new ArrayList<>();
    private Map<Long, List<SimpleCard>> discountCards = new HashMap<>();
    private static List<String> DiscountCardPara = new ArrayList<>();

    @LocalServerPort
    private static int port = 8080;
    @Autowired
    private ObjectMapper objectMapper;

    private static ObjectMapper staticMapper = new ObjectMapper();

    @BeforeAll
    public static void createCustomers(){
        RestAssured.port = port;

        for (int i=0; i<3; i++){
            ObjectNode postbody = staticMapper.createObjectNode().put("birthdate","1999-10-11");
            Response response = given().contentType("application/json").body(postbody.toString()).post("/customers");
            customerIds.add(Long.valueOf(response.then().extract().path("id").toString()));
        }
        DiscountCardPara = List.of("id", "customerId", "type", "validFrom", "validFor");
    }
    private static Stream<Arguments> provideDataForAddDiscountCard(){
        return Stream.of(
                Arguments.of(25,"2022-10-10","1y"),
                //test invalid inputs
                Arguments.of(30,"2000-10-10","1y"),
                Arguments.of(25,"2000-10-10","2y"),
                //customer not existed
                Arguments.of(50,"2000-10-10","1y"),
                //card existed (overlapping timeframe)
                Arguments.of(50,"2022-12-10","30d")
        );
    }
    @ParameterizedTest
    @MethodSource("provideDataForAddDiscountCard")
    public void addDiscountCardTest(int type, String validFrom, String validFor) throws JSONException {

        //create discount cards for an existed customer
        Long customerId = customerIds.get(0);
        ObjectNode postbody = objectMapper.createObjectNode().put("validFrom",validFrom).put("validFor",validFor).put("type",type);
        Response response = given().contentType("application/json").body(postbody.toString()).post(path(customerId));

        //create discount card for a non-existed customer
        Long randomId = getRandomWithExclusion(new Random(),0L,Long.MAX_VALUE,customerIds);
        ObjectNode randomIdpostbody = objectMapper.createObjectNode().put("validFrom",validFrom).put("validFor",validFor).put("type",type);
        given().contentType("application/json").body(randomIdpostbody.toString()).post(path(randomId)).then().assertThat().statusCode(404);


        //3 possible output statuses: 400, 409, 201 (404 is not covered since response is returned for post-request with existed customer Id)
        //invalid customer Id
        if(!customerIds.contains(customerId)){
            response.then().assertThat().statusCode(404);
        }
        //invalid input values for discount card
        else if((type!=25 & type!=50) || (validFor!="30d" & validFor!="1y")){
            response.then().assertThat().statusCode(400);
        }
        else{
            List<SimpleCard> existedCards = discountCards.get(customerId);
            SimpleCard current = new SimpleCard(validFrom,validFor);
            if(!current.getError() && existedCards!=null){
                existedCards.forEach(card ->{
                    //check if overlapping with existed card
                    if(!card.getError() && card.contains(current)){
                        response.then().assertThat().statusCode(409);
                    }
                });
            }
            //if no errors found, add simple card to list -> in order to check overlapping timeframes later on
            if(response.statusCode()!=409){
                if(discountCards.get(customerId)==null){
                    discountCards.put(customerId, new ArrayList<>());
                }
                discountCards.get(customerId).add(current);
                response.then().assertThat().statusCode(201);

                //get keys from response body
                JSONObject myObject = new JSONObject(response.getBody().asString());
                List<String> keys = new ArrayList<>();
                myObject.keys().forEachRemaining(key -> keys.add(key.toString()));

                //assure that returned response has the defined schema
                DiscountCardPara.forEach(para -> {
                    assertThat(keys.contains(para)).isTrue();
                });
            }
        }
    }

    private static Stream<Arguments> provideDataForGetDiscountCard(){
        return Stream.of(
                Arguments.of(25,"2022-10-10","1y"),
                Arguments.of(50,"2000-10-10","1y"),
                Arguments.of(50,"2020-10-10","1y")
        );
    }

    @ParameterizedTest
    @MethodSource("provideDataForGetDiscountCard")
    public void getDiscountCardTest_validInputs(int type, String validFrom, String validFor) throws JSONException {

        Long customerId = customerIds.get(1);
        //successful post 201
        ObjectNode postbody = objectMapper.createObjectNode().put("validFrom", validFrom).put("validFor", validFor).put("type", type);
        given().contentType("application/json").body(postbody.toString()).post(path(customerId)).then().assertThat().statusCode(201);


        Response getResponse = given().when().get(path(customerId));
        JSONArray responseArray = new JSONArray(getResponse.getBody().asString());
        if(responseArray.length()==0){
            getResponse.then().assertThat().statusCode(404);
        }
        else{
            getResponse.then().assertThat().statusCode(200);

            //array of keys for all returned cards by get-request
            List<List<String>> keys = new ArrayList<>();
            for(int i = 0 ; i < responseArray.length(); i++) {
                List<String> keysOfCard = new ArrayList<>();
                responseArray.getJSONObject(i).keys().forEachRemaining(key -> keysOfCard.add(key.toString()));
                keys.add(keysOfCard);
            }

            //assure that all returned cards have the defined schema
            keys.forEach(card -> {
                DiscountCardPara.forEach(para -> {
                    assertThat(card.contains(para)).isTrue();
                });
            });

        }
    }
    private Long getRandomWithExclusion(Random rnd, Long start, Long end, List<Long> exclude) {
        Long random = start + rnd.nextLong(end - start + 1 - exclude.size());
        for (Long ex : exclude) {
            if (random < ex) {
                break;
            }
            random++;
        }
        return random;
    }

    @Test
    public void getDiscountCardTest_invalidInputs() throws JSONException {
        //invalid id
        given().contentType("application/json").when().get("/customers/abc/discountcards").then().assertThat().statusCode(400);

        //customer not existed
        Long randomId = getRandomWithExclusion(new Random(),0L,Long.MAX_VALUE,customerIds);
        given().when().get(path(randomId)).then().assertThat().statusCode(404);

        //no card found
        Response response = given().when().get(path(customerIds.get(2)));
        System.out.println(response.getBody().asString());

        JSONArray responseArray = new JSONArray(response.getBody().asString());
        if(responseArray.length()==0){
            response.then().assertThat().statusCode(404);
        }

    }



    protected static class SimpleCard{
        protected LocalDate validFrom;
        protected LocalDate validUntil;
        protected boolean error=false;

        protected SimpleCard(String validFrom, String validFor){
            try {
                LocalDate date = LocalDate.parse(validFrom, DateTimeFormatter.ISO_LOCAL_DATE);
                this.validFrom = date;
            }
            catch (DateTimeParseException e) {
                error = true;
            }
            if(!error){
                if(validFor.equals("30d")){
                    this.validUntil = this.validFrom.plusDays(30);
                }
                else if (validFor.equals("1y")){
                    this.validUntil = this.validFrom.plusYears(1);
                }
            }
        }
        protected boolean getError(){
            return this.error;
        }
        protected boolean contains(SimpleCard other){
            if(error){
                return false;
            }
            if(this.validFrom.isBefore(other.validFrom) || this.validFrom.isEqual(other.validFrom)){
                if(!this.validUntil.isBefore(this.validFrom)){
                    return true;
                }
            }
            else{
                if(!other.validUntil.isBefore(this.validFrom)){
                    return true;
                }
            }
            return false;
        }
    }
}