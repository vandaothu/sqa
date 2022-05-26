package de.rwth.swc.sqa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
public class TicketValidationTest {

    @Autowired
    private ObjectMapper objectMapper;
    private static final String PATH = "/tickets/validate";


    @LocalServerPort
    private static int port = 8080;

    private static ObjectMapper staticMapper = new ObjectMapper();
    private static List<Long> ticketIds = new ArrayList<>();
    private static Map<Long,DiscountCardTest.SimpleCard> discountCards = new HashMap<>();


    @BeforeAll
    public static void createTickets() throws JSONException {
        RestAssured.port = port;

        //create customer
        ObjectNode postcustomer = staticMapper.createObjectNode().put("birthdate","1999-10-11");
        var customerId = given().contentType("application/json").body(postcustomer.toString()).post("/customers").then().extract().path("id");

        ObjectNode[] cardrequest = new ObjectNode[2];
        //create discount card request
        cardrequest[0] = staticMapper.createObjectNode().put("validFrom","2022-5-26").put("validFor", "1y").put("type",50);
        //create request of very old discound card
        cardrequest[1] = staticMapper.createObjectNode().put("validFrom","2000-5-26").put("validFor", "1y").put("type",50);
        for(int i=0; i<2; i++){
            Response response = given().contentType("application/json").body(cardrequest[i].toString()).post("/customers/"+customerId+"/discountcards");
            JSONObject jsonCard = new JSONObject(response.getBody().asString());

            //save pairs of (cardId, simple timeframe of card)
            discountCards.put(Long.valueOf(jsonCard.get("id").toString()),new DiscountCardTest.SimpleCard(jsonCard.get("validFrom").toString(),jsonCard.get("validFor").toString()));
        }

        //buy ticket
        


    }



}