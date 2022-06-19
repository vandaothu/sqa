package de.rwth.swc.sqa.api.models;


import java.time.LocalDate;
import java.util.Objects;

public class DiscountCard extends DiscountCardRequestBody {
    public long id;
    public long customerId;

    public DiscountCard(long cardId, long customerId, int type, String validFrom, String validFor) throws Exception {
        try {
            LocalDate.parse(validFrom, dateFormatter);
        } catch (Exception e) {
            throw new Exception();
        }

        if (type != 25 && type != 50) {
            throw new Exception();
        }

        if (!Objects.equals(validFor, "30d") && !Objects.equals(validFor, "1y")) {
            throw new Exception();
        }

        this.id = cardId;
        this.customerId = customerId;
        this.type = type;
        this.validFrom = validFrom;
        this.validFor = validFor;
    }

    public LocalDate convertFrom() {
        return LocalDate.parse(validFrom, dateFormatter);
    }

    public LocalDate convertUntil() {
        if (Objects.equals(this.validFor, "30d")) {
            return this.convertFrom().plusDays(30);
        } else {
            return this.convertFrom().plusYears(1);
        }
    }
}
