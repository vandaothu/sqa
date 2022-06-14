package de.rwth.swc.sqa.api.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;

public class Ticket implements CustomDateTimeFormatter {
    public long id;
    public String validFrom;
    public String birthdate;
    public LocalDate LDBirthDate;
    public String validFor;
    public boolean disabled;
    public int discountCard = -1;
    public String zone;
    public boolean student;
    static String[] zoneEnum = new String[]{"A", "B", "C"};

    public Ticket(long id, String birthdate, String validFrom, String validFor, boolean disabled, int discountCard, String zone,
                  boolean student) throws Exception {
        this.id = id;
        this.birthdate = birthdate;
        this.validFor = validFor;
        this.validFrom = validFrom;
        this.disabled = disabled;
        this.discountCard = discountCard;
        this.zone = zone;
        this.student = student;
        this.checkTimeFormat();
        this.checkZone();
        this.checkTicketExistence();
        this.checkDiscountCard();

    }

    private void checkZone() throws Exception {
        boolean isZoneExist = !Objects.isNull(this.zone);
        boolean isValidFor1h = Objects.equals(this.validFor, "1h");
        if ((isZoneExist != isValidFor1h)
            || (isZoneExist && !Arrays.asList(zoneEnum).contains(this.zone))) {
            throw new Exception();
        }
    }

    private void checkTimeFormat() throws Exception {
        this.LDBirthDate = LocalDate.parse(this.birthdate, dateFormatter);
        try {
            LocalDateTime.parse(this.validFrom, dateTimeFormatter);
        } catch (Exception e) {
            throw new Exception();
        }

        if (!Objects.equals(this.validFor, "1h")
            && !Objects.equals(this.validFor, "1d")
            && !Objects.equals(this.validFor, "30d")
            && !Objects.equals(this.validFor, "1y")
        ) {
            throw new Exception();
        }
    }

    private void checkTicketExistence() throws Exception {
        LocalDate currentDate = LocalDate.now();
        boolean isSenior = false;
        boolean isStudent = false;
        boolean isChild = false;
        long age = ChronoUnit.YEARS.between(this.LDBirthDate, currentDate);

        if (this.student && (age < 14 || age >= 28)) {
            throw new Exception();
        }

        if (age >= 60) {
            isSenior = true;
        } else if (this.student) {
            isStudent = true;
        } else if (age < 14) {
            isChild = true;
        }

        if ((isSenior && Objects.equals(this.validFor, "1h"))
            || (isStudent && (Objects.equals(this.validFor, "1h")
                              || Objects.equals(this.validFor, "1d")))
            || (isChild && Objects.equals(this.validFor, "1d"))
        ) {
            throw new Exception();
        }
    }

    private void checkDiscountCard() throws Exception {
        if (this.discountCard != -1) {
            if ((!Objects.equals(this.validFor, "30d") && !Objects.equals(this.validFor, "1y"))
                || (this.discountCard != 25 && this.discountCard != 50)) {
                throw new Exception();
            }
        }
    }

    public LocalDateTime getFrom() {
        return LocalDateTime.parse(this.validFrom, dateTimeFormatter);
    }

    public LocalDateTime getUntil() {
        if (Objects.equals(this.validFor, "1h")) {
            return this.getFrom().plusHours(1);
        } else if (Objects.equals(this.validFor, "1d")) {
            return this.getFrom().plusDays(1);
        } else if (Objects.equals(this.validFor, "30d")) {
            return this.getFrom().plusDays(30);
        } else {
            return this.getFrom().plusYears(1);
        }
    }
}
