package de.rwth.swc.sqa.api.models;


import java.time.LocalDate;
import java.util.Objects;

public class DiscountCard extends DiscountCardRequestBody {
    public long id;
    public long customerId;
    public LocalDate from;
    public LocalDate until;

    public DiscountCard(long cardId, long customerId, int type, String validFrom, String validFor) throws Exception {
        this.from = LocalDate.parse(validFrom, dateFormatter);
        if (type != 25 && type != 50) {
            throw new Exception();
        }

        if (Objects.equals(validFor, "30d")) {
            this.until = this.from.plusDays(30);
        } else if (Objects.equals(validFor, "1y")) {
            this.until = this.from.plusYears(1);
        } else {
            throw new Exception();
        }

        this.id = cardId;
        this.customerId = customerId;
        this.type = type;
        this.validFrom = validFrom;
        this.validFor = validFor;
    }
}