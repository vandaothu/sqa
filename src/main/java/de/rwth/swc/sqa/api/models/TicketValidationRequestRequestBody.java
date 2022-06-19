package de.rwth.swc.sqa.api.models;

public class TicketValidationRequestRequestBody {
    public long ticketId = -1;
    public String zone;
    public String date;
    public boolean disabled = false;
    public long discountCardId = -1;
    public boolean student = false;
}
