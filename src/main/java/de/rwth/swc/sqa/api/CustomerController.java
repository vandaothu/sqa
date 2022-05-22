package de.rwth.swc.sqa.api;

import de.rwth.swc.sqa.api.models.Customer;
import de.rwth.swc.sqa.api.models.CustomerRequestBody;
import de.rwth.swc.sqa.api.models.DiscountCard;
import de.rwth.swc.sqa.api.models.DiscountCardRequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
@RequestMapping("/customers")
public class CustomerController implements CustomersApi {
    @Autowired
    Database db;

    @GetMapping("")
    public ResponseEntity<String> test() {
        return new ResponseEntity<String>( db.customerList.keySet().toString(), HttpStatus.valueOf(200));
    }

    @PostMapping("")
    public ResponseEntity<Customer> addCustomer(@RequestBody CustomerRequestBody requestBody) {
        try {
            long id = this.generateId("customers");
            Customer c = new Customer(id, requestBody.birthdate, requestBody.disabled);
            db.customerList.put(c.id, c);
            return new ResponseEntity<Customer>(c, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<Customer>(HttpStatus.valueOf(400));
        }
    }

    @PostMapping("/{customerId}/discountcards")
    public ResponseEntity<DiscountCard> addDiscountCardToCustomer(
       @RequestBody DiscountCardRequestBody requestBody, @PathVariable(value = "customerId") long customerId
    ) {
        if (!db.customerList.containsKey(customerId)) {
            return new ResponseEntity<DiscountCard>(HttpStatus.valueOf(404));
        }

        try {
            long id = this.generateId("discountCard");
            DiscountCard discountCard = new DiscountCard(id, customerId, requestBody.type, requestBody.validFrom, requestBody.validFor);
            this.checkDiscountCardConflict(discountCard, customerId);
            db.discountCardList.put(discountCard.id, discountCard);
            return new ResponseEntity<DiscountCard>(discountCard, HttpStatus.CREATED);
        } catch (Exception e) {
            if (Objects.equals(e.getMessage(), "conflict")) {
                return new ResponseEntity<DiscountCard>(HttpStatus.valueOf(409));
            }
            return new ResponseEntity<DiscountCard>(HttpStatus.valueOf(400));
        }

    }

    @GetMapping("/{customerId}/discountcards")
    public ResponseEntity<ArrayList<DiscountCard>> getCustomerDiscountCards(@PathVariable("customerId") long customerId) {
        if (!db.customerList.containsKey(customerId)) {
            return new ResponseEntity<ArrayList<DiscountCard>>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<ArrayList<DiscountCard>>(this.getDiscountCards(customerId), HttpStatus.OK);
    }

    private long generateId(String listName) {
        long candidate = 0;
        if (Objects.equals(listName, "customers")) {
            while (db.customerList.containsKey(candidate)) {
                candidate += 1;
            }
        } else {
            while (db.discountCardList.containsKey(candidate)) {
                candidate += 1;
            }
        }

        return candidate;
    }

    private ArrayList<DiscountCard> getDiscountCards(long customerId) {
        ArrayList<DiscountCard> result = new ArrayList<DiscountCard>();

        for (DiscountCard card : db.discountCardList.values()) {
            if (card.customerId == customerId) {
                result.add(card);
            }
        }

        return result;
    }

    private void checkDiscountCardConflict(DiscountCard cardToCheck, long customerId) throws Exception {
        for (DiscountCard card : this.getDiscountCards(customerId)) {
            if (!(ChronoUnit.DAYS.between(card.until, cardToCheck.from) > 0
                  || ChronoUnit.DAYS.between(cardToCheck.until, card.from) > 0)) {
                throw new Exception("conflict");
            }
        }
    }

}
