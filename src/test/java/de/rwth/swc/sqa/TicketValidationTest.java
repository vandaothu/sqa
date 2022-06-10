package de.rwth.swc.sqa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
public class TicketValidationTest {

    @Autowired
    private ObjectMapper objectMapper;
    private static final String PATH = "/tickets/validate";


    @LocalServerPort
    private static int port = 8080;

    private static ObjectMapper staticMapper = new ObjectMapper();
    private static List<Long> discountCards = new ArrayList<>();
    private static Map<Long,Ticket> tickets = new LinkedHashMap<>();

    @BeforeAll
    public static void createTickets() {
        RestAssured.port = port;
        boolean jsonError = false;

        //create customer
        ObjectNode postcustomer = staticMapper.createObjectNode().put("birthdate","1999-10-11");
        String[] customerIds = new String[2];
        customerIds[0] = given().contentType("application/json").body(postcustomer.toString()).post("/customers").then().extract().path("id").toString();
        customerIds[1] = given().contentType("application/json").body(postcustomer.toString()).post("/customers").then().extract().path("id").toString();

        ObjectNode[] cardrequest = new ObjectNode[2];
        //create discount card request
        cardrequest[0] = staticMapper.createObjectNode().put("validFrom","2022-05-26").put("validFor", "1y").put("type",50);
        //create request of very old discound card
        cardrequest[1] = staticMapper.createObjectNode().put("validFrom","2000-05-26").put("validFor", "1y").put("type",50);
        for(int i=0; i<2; i++){
            Response response = given().contentType("application/json").body(cardrequest[i].toString()).post("/customers/"+customerIds[i]+"/discountcards");
            if(response.statusCode()==201){
                JSONObject jsonCard = null;
                try {
                    jsonCard = new JSONObject(response.getBody().asString());
                    //save pairs of (cardId, simple timeframe of card)
                    discountCards.add(Long.valueOf(jsonCard.get("id").toString()));
                } catch (JSONException e) {
                    jsonError=true;
                }
            }
        }

        //buy ticket
        ObjectNode[] ticketrequest = new ObjectNode[4];
        //create an expired ticket
        ticketrequest[0] = staticMapper.createObjectNode().put("birthdate","1999-10-11").put("validFrom","2010-05-26T10:38:59").put("validFor","1h").put("zone", "A");
        //create a valid ticket with no discount
        ticketrequest[1] = staticMapper.createObjectNode().put("birthdate","1999-10-11").put("validFrom","2022-05-26T10:38:59").put("validFor","1y");
        //create a valid ticket with discount card
        ticketrequest[2] = staticMapper.createObjectNode().put("birthdate","1999-10-11").put("validFrom","2022-05-26T10:38:59").put("validFor","1y").put("discountCard",true);
        //create a ticket for disabled person
        ticketrequest[3] = staticMapper.createObjectNode().put("birthdate","1999-10-11").put("validFrom","2010-05-26T10:38:59").put("validFor","30d").put("disabled", true).put("student", true);
        for(int i=0; i<4; i++){
            Response response = given().contentType("application/json").body(ticketrequest[i].toString()).post("/tickets");
            if(response.statusCode()==201) {
                JSONObject jsonTicket = null;
                try {
                    jsonTicket = new JSONObject(response.getBody().asString());
                    //save pairs of (ticketId, Ticket)
                    tickets.put(Long.valueOf(jsonTicket.get("id").toString()), new Ticket(jsonTicket));
                } catch (JSONException e) {
                    jsonError=true;
                }

            }
        }
        assertThat(jsonError).isFalse();
    }

    @Test
    public void inconsistentDataValidationTest(){
        Long ticketId = tickets.keySet().iterator().next();
        Ticket ticket = tickets.get(ticketId);

        ObjectNode post_inconsistent_zone = objectMapper.createObjectNode().put("ticketId", ticketId).
                put("zone", "B").
                put("date", ticket.getStartTime());
        //send request to validate ticket with inconsistency in zone
        given().contentType("application/json").body(post_inconsistent_zone.toString()).post(PATH).then().assertThat().statusCode(403);

        ObjectNode post_inconsistent_disabled = objectMapper.createObjectNode().put("ticketId", ticketId).
                put("zone", ticket.zone).
                put("date", ticket.getStartTime()).
                put("disabled", true);
        //disabled people can use normal ticket
        given().contentType("application/json").body(post_inconsistent_disabled.toString()).post(PATH).then().assertThat().statusCode(200);

        Long special_ticketId = tickets.keySet().stream().skip(3).findFirst().orElse(null);
        post_inconsistent_disabled.remove("disabled");
        post_inconsistent_disabled.remove("ticketId");
        post_inconsistent_disabled.put("ticketId",special_ticketId);
        //normal people can't use ticket for the disabled
        given().contentType("application/json").body(post_inconsistent_disabled.toString()).post(PATH).then().assertThat().statusCode(403);


        ObjectNode post_inconsistent_student = objectMapper.createObjectNode().put("ticketId", ticketId).
                put("zone", ticket.zone).
                put("date", ticket.getStartTime()).
                put("student", true);
        //student can use normal ticket
        given().contentType("application/json").body(post_inconsistent_student.toString()).post(PATH).then().assertThat().statusCode(200);
        post_inconsistent_student = objectMapper.createObjectNode().put("ticketId", special_ticketId).
                put("zone", ticket.zone).
                put("date", ticket.getStartTime()).
                put("student", false);
        //normal people can't use student ticket
        given().contentType("application/json").body(post_inconsistent_student.toString()).post(PATH).then().assertThat().statusCode(403);
    }

    @Test
    public void dateValidationTest(){
        Iterator<Long> iter = tickets.keySet().iterator();
        Long oldTicketId = iter.next();
        Ticket oldTicket = tickets.get(oldTicketId);

        Long ticketId = iter.next();
        Ticket ticket = tickets.get(ticketId);

        //correct ticket information, used on wrong date
        ObjectNode post_wrongdate = objectMapper.createObjectNode().put("ticketId", oldTicketId).
                put("zone", oldTicket.zone).
                put("date", ticket.getStartTime());

        //send request to validate ticket that is used in wrong timeframe
        given().contentType("application/json").body(post_wrongdate.toString()).post(PATH).then().assertThat().statusCode(403);

        //correct ticket information, used in correct timeframe
        ObjectNode post_correctdate = objectMapper.createObjectNode().put("ticketId", ticketId).
                put("zone", ticket.zone).
                put("date", ticket.getStartTime());
        //send request to validate ticket that is used in correct timeframe
        given().contentType("application/json").body(post_correctdate.toString()).post(PATH).then().assertThat().statusCode(200); //BVT - lower bound
        post_correctdate.remove("date"); post_correctdate.put("date", ticket.getEndTime());
        given().contentType("application/json").body(post_correctdate.toString()).post(PATH).then().assertThat().statusCode(200); //BVT - upper bound
    }

    @Test
    public void discountCardValidationTest(){
        Iterator<Long> iter = tickets.keySet().iterator();
        Long first = iter.next();
        Long NO_discount_ticketID = iter.next();
        Long discount_ticketID = iter.next();
        Ticket NO_discount_ticket = tickets.get(NO_discount_ticketID);
        Ticket discount_ticket = tickets.get(discount_ticketID);

        Long valid_discountCard = discountCards.get(0);
        Long expired_discountCard = discountCards.get(1);


        //validation request for ticket with NO discount card

        ObjectNode post_ticketWithNoDiscount = objectMapper.createObjectNode().put("ticketId", NO_discount_ticketID).
                put("zone", NO_discount_ticket.zone).
                put("date", NO_discount_ticket.getStartTime()).
                put("discountCard", valid_discountCard);

        //given a discountCard -> card-holder can use non-discounted ticket
        given().contentType("application/json").body(post_ticketWithNoDiscount.toString()).post(PATH).then().assertThat().statusCode(200);

        post_ticketWithNoDiscount.remove("discountCard");
        //given no discountCard
        given().contentType("application/json").body(post_ticketWithNoDiscount.toString()).post(PATH).then().assertThat().statusCode(200);




        //validation request for ticket WITH a discount card

        ObjectNode post_ticketWithDiscount = objectMapper.createObjectNode().put("ticketId", discount_ticketID).
                put("zone", discount_ticket.zone).
                put("date", discount_ticket.getStartTime());
        //given no discountCard
        given().contentType("application/json").body(post_ticketWithDiscount.toString()).post(PATH).then().assertThat().statusCode(403);


        post_ticketWithDiscount.put("discountCardId", expired_discountCard);
        //given expired discountCard
        given().contentType("application/json").body(post_ticketWithDiscount.toString()).post(PATH).then().assertThat().statusCode(403);


        post_ticketWithDiscount.remove("discountCard");
        post_ticketWithDiscount.put("discountCardId", valid_discountCard);
        //given valid discountCard
        given().contentType("application/json").body(post_ticketWithDiscount.toString()).post(PATH).then().assertThat().statusCode(200);

    }

    private static class Ticket{
        protected Long id;
        protected LocalDateTime validFrom;
        protected LocalDateTime validTil;
        protected boolean disabled;
        protected String zone;
        protected boolean student;
        protected boolean discountCard;

        protected boolean parsingJson = false;


        protected Ticket(JSONObject jsonTicket){
            try {
                this.id = Long.valueOf(jsonTicket.get("id").toString());
                this.validFrom =  LocalDateTime.parse(jsonTicket.get("validFrom").toString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                switch (jsonTicket.get("validFor").toString()){
                    case "1h":
                        this.validTil = this.validFrom.plusHours(1);
                        break;
                    case "1d":
                        this.validTil = this.validFrom.plusDays(1);
                        break;
                    case "30d":
                        this.validTil = this.validFrom.plusDays(30);
                        break;
                    case "1y":
                        this.validTil = this.validFrom.plusYears(1);
                        break;
                    default:
                        this.validTil = this.validFrom;
                        break;
                }
                this.disabled = (Boolean) jsonTicket.get("disabled");
                this.student = (Boolean) jsonTicket.get("student");
                this.discountCard = (Boolean) jsonTicket.get("discountCard");
                if(jsonTicket.get("zone").toString()!="null"){
                    this.zone = jsonTicket.get("zone").toString();
                }
                else{
                    this.zone = "C";
                }

            } catch (JSONException e) {
                this.parsingJson = true;
            }
        }
        protected String getStartTime(){
            return this.validFrom.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        protected String getEndTime(){
            return this.validTil.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

    }


}