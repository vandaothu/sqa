package de.rwth.swc.sqa.api.models;

import java.time.LocalDateTime;

import static de.rwth.swc.sqa.api.models.CustomDateTimeFormatter.dateTimeFormatter;

public class TicketValidationRequest {
    public long ticketId;
    public String zone;
    public String date;
    public boolean disabled;
    public long discountCardId;
    public boolean student;

    public TicketValidationRequest(TicketValidationRequestRequestBody requestBody) throws Exception {
        this.ticketId = requestBody.ticketId;
        this.zone = requestBody.zone;
        this.date = requestBody.date;
        this.disabled = requestBody.disabled;
        this.discountCardId = requestBody.discountCardId;
        this.student = requestBody.student;

        this.checkId();
        this.checkZone();
        this.checkDate();
    }

    private void checkId() throws Exception {
        if (this.ticketId == - 1) {
            throw new Exception();
        }
    }

    private void checkZone() throws Exception {
        if (!this.zone.equals("A")
            && !this.zone.equals("B")
            && !this.zone.equals("C")
        ) {
            throw new Exception();
        }
    }

    private void checkDate() {
        LocalDateTime date = LocalDateTime.parse(this.date, dateTimeFormatter);
    }
}
