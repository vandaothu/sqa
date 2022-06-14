package de.rwth.swc.sqa.api.models;

public class TicketRequest {
    public String validFrom;
    public String birthdate;
    public String validFor;
    public boolean disabled = false;
    public int discountCard = -1;
    public String zone;
    public boolean student = false;
}
