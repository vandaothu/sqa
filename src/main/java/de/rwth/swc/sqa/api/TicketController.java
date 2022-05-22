package de.rwth.swc.sqa.api;

import de.rwth.swc.sqa.api.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Controller
@RequestMapping("/tickets")
public class TicketController implements TicketsApi, CustomDateTimeFormatter {

    @Autowired
    Database db;


    @GetMapping("/abc")
    public ResponseEntity<String> doSth() {
        return new ResponseEntity<String>(db.customerList.keySet().toString(), HttpStatus.OK);
    }

    @PostMapping("")
    public ResponseEntity<Ticket> addTicket(@RequestBody TicketRequest requestBody) {
        long ticketId = this.generateId();
        try {
            Ticket t = new Ticket(
               ticketId,
               requestBody.birthdate,
               requestBody.validFrom,
               requestBody.validFor,
               requestBody.disabled,
               requestBody.discountCard,
               requestBody.zone,
               requestBody.student
            );
            db.ticketList.put(ticketId, t);
            return new ResponseEntity<Ticket>(t, HttpStatus.CREATED);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<Ticket>(HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateTicket(@RequestBody TicketValidationRequest requestBody) {
        try {
            Ticket t = this.getTicketToCheck(requestBody);
            this.checkDate(requestBody, t);
            this.checkZone(requestBody, t);
            this.checkDisabled(requestBody, t);
            return new ResponseEntity<String>("Ticket is valid", HttpStatus.OK);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return new ResponseEntity<String>("Ticket is not valid", HttpStatus.FORBIDDEN);
        }

    }

    private Ticket getTicketToCheck(TicketValidationRequest requestBody) throws Exception {
        long id = requestBody.ticketId;
        if (!db.ticketList.containsKey(id)) {
            throw new Exception();
        }
        return db.ticketList.get(requestBody.ticketId);
    }

    private void checkDate(TicketValidationRequest requestBody, Ticket t) throws Exception {
        LocalDateTime date = LocalDateTime.parse(requestBody.date, dateTimeFormatter);
        if (ChronoUnit.HOURS.between(t.from, date) < 0 || ChronoUnit.HOURS.between(date, t.until) < 0) {
            throw new Exception();
        }
    }

    private void checkZone(TicketValidationRequest requestBody, Ticket t) throws Exception {
        String zone = requestBody.zone;
        if ((Objects.equals(t.zone, "A") && !Objects.equals(zone, "A"))
            || ((Objects.equals(t.zone, "B") && Objects.equals(zone, "C")))
        ) {
            throw new Exception();
        }
    }

    private void checkDisabled(TicketValidationRequest requestBody, Ticket t) throws Exception {
        boolean disabled = requestBody.disabled;
        if (disabled && !t.disabled) {
            throw new Exception();
        }
    }

    private long generateId() {
        long candidate = 0;
        while (db.ticketList.containsKey(candidate)) {
            candidate += 1;
        }

        return candidate;
    }
}
