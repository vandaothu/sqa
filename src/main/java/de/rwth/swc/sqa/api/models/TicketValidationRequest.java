package de.rwth.swc.sqa.api.models;

public class TicketValidationRequest {
    public long ticketId;
    public String zone;
    public String date;
    public boolean disabled = false;
    public long discountCardId = -1;
    public boolean student = false;

    public TicketValidationRequest(int ticketId, String zone, String date) {
        this.ticketId = ticketId;
        this.zone = zone;
        this.date = date;
    }
}
