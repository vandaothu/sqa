package de.rwth.swc.sqa.api.models;

public class TicketValidationRequest {
    public long ticketId;
    public String zone;
    public String date;
    public boolean disabled;
    public long discountCardId;
    public boolean student;

    public TicketValidationRequest(int ticketId, String zone, String date) {
        this.ticketId = ticketId;
        this.zone = zone;
        this.date = date;
    }
}
