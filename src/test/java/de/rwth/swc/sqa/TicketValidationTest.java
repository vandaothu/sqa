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
    private static Map<Long,Ticket> tickets = new HashMap<>();

    @BeforeAll
    public static void createTickets() throws JSONException {
        RestAssured.port = port;

        //create customer
        ObjectNode postcustomer = staticMapper.createObjectNode().put("birthdate","1999-10-11");
        String[] customerIds = new String[2];
        customerIds[0] = given().contentType("application/json").body(postcustomer.toString()).post("/customers").then().extract().path("id").toString();
        customerIds[1] = given().contentType("application/json").body(postcustomer.toString()).post("/customers").then().extract().path("id").toString();

        ObjectNode[] cardrequest = new ObjectNode[2];
        //create discount card request
        cardrequest[0] = staticMapper.createObjectNode().put("validFrom","2022-5-26").put("validFor", "1y").put("type",50);
        //create request of very old discound card
        cardrequest[1] = staticMapper.createObjectNode().put("validFrom","2000-5-26").put("validFor", "1y").put("type",50);
        for(int i=0; i<2; i++){
            Response response = given().contentType("application/json").body(cardrequest[i].toString()).post("/customers/"+customerIds[i]+"/discountcards");
            if(response.statusCode()==201){
                JSONObject jsonCard = new JSONObject(response.getBody().asString());
                //save pairs of (cardId, simple timeframe of card)
                discountCards.add(Long.valueOf(jsonCard.get("id").toString()));
            }
        }

        //buy ticket
        ObjectNode[] ticketrequest = new ObjectNode[3];
        //create an expired ticket
        ticketrequest[0] = staticMapper.createObjectNode().put("birthdate","1999-10-11").put("validFrom","2010-5-26T10:38:59").put("validFor","1y").put("zone","A");
        //create a valid ticket with no discount
        ticketrequest[1] = staticMapper.createObjectNode().put("birthdate","1999-10-11").put("validFrom","2022-5-26T10:38:59").put("validFor","1y").put("zone","B");
        //create a valid ticket with discount card
        ticketrequest[2] = staticMapper.createObjectNode().put("birthdate","1999-10-11").put("validFrom","2010-5-26T10:38:59").put("validFor","1y").put("discountCard",true).put("zone","C");
        for(int i=0; i<3; i++){
            Response response = given().contentType("application/json").body(ticketrequest[i].toString()).post("/tickets");
            if(response.statusCode()==201) {
                System.out.println(response.getBody().asString());
                JSONObject jsonTicket = new JSONObject(response.getBody().asString());
                //save pairs of (ticketId, Ticket)
                tickets.put(Long.valueOf(jsonTicket.get("id").toString()), new Ticket(jsonTicket));
            }
        }
        System.out.println(tickets);

    }

    @Test
    public void inconsistentDataValidationTest(){
        Long ticketId = tickets.keySet().iterator().next();
        Ticket ticket = tickets.get(ticketId);


        List<String> zones = List.of("A", "B", "C");
        zones.remove(ticket.zone);
        ObjectNode post_inconsistent_zone = objectMapper.createObjectNode().put("ticketId", ticketId).
                put("zone", zones.get(0)).
                put("date", ticket.getStartTime());
        //send request to validate ticket with inconsistency in zone
        given().contentType("application/json").body(post_inconsistent_zone.toString()).post(PATH).then().assertThat().statusCode(403);

        ObjectNode post_inconsistent_disabled = objectMapper.createObjectNode().put("ticketId", ticketId).
                put("zone", ticket.zone).
                put("date", ticket.getStartTime()).
                put("disabled", !ticket.disabled);
        //send request to validate ticket with inconsistency in disability
        given().contentType("application/json").body(post_inconsistent_disabled.toString()).post(PATH).then().assertThat().statusCode(403);

        ObjectNode post_inconsistent_student = objectMapper.createObjectNode().put("ticketId", ticketId).
                put("zone", ticket.zone).
                put("date", ticket.getStartTime()).
                put("student", !ticket.student);
        //send request to validate ticket with inconsistency in student status
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
        //send request to validate ticket that is used in wrong timeframe
        given().contentType("application/json").body(post_correctdate.toString()).post(PATH).then().assertThat().statusCode(200);
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
        //given a discountCard
        given().contentType("application/json").body(post_ticketWithNoDiscount.toString()).post(PATH).then().assertThat().statusCode(403);
        post_ticketWithNoDiscount.remove("discountCard");
        //given no discountCard
        given().contentType("application/json").body(post_ticketWithNoDiscount.toString()).post(PATH).then().assertThat().statusCode(200);



        //validation request for ticket WITH a discount card

        ObjectNode post_ticketWithDiscount = objectMapper.createObjectNode().put("ticketId", discount_ticketID).
                put("zone", discount_ticket.zone).
                put("date", discount_ticket.getStartTime());
        //given no discountCard
        given().contentType("application/json").body(post_ticketWithDiscount.toString()).post(PATH).then().assertThat().statusCode(403);

        post_ticketWithDiscount.put("discountCard",expired_discountCard);
        //given expired discountCard
        given().contentType("application/json").body(post_ticketWithDiscount.toString()).post(PATH).then().assertThat().statusCode(403);

        post_ticketWithDiscount.remove("discountCard");
        post_ticketWithDiscount.put("discountCard",valid_discountCard);
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
        final private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-ddTHH:mm::ss");

        protected boolean parsingJson = false;


        protected Ticket(JSONObject jsonTicket){
            try {
                this.id = Long.valueOf(jsonTicket.get("id").toString());
                this.validFrom =  LocalDateTime.parse(jsonTicket.get("validFrom").toString(), formatter);
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
                this.zone = jsonTicket.get("zone").toString();

            } catch (JSONException e) {
                this.parsingJson = true;
            }
        }
        protected String getStartTime(){
            return this.validFrom.format(formatter);
        }
    }


}