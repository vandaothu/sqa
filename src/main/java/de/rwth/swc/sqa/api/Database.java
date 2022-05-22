package de.rwth.swc.sqa.api;

import de.rwth.swc.sqa.api.models.Customer;
import de.rwth.swc.sqa.api.models.DiscountCard;
import de.rwth.swc.sqa.api.models.Ticket;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class Database {
    public HashMap<Long, Customer> customerList = new HashMap<Long, Customer>();
    public HashMap<Long, DiscountCard> discountCardList = new HashMap<Long, DiscountCard>();
    public HashMap<Long, Ticket> ticketList = new HashMap<Long, Ticket>();
}
