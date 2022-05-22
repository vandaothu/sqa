package de.rwth.swc.sqa.api.models;

public class TicketRequest {
    public String validFrom;
    public String birthdate;
    public String validFor;
    public boolean disabled = false;
    public boolean discountCard = false;
    public String zone;
    public boolean student = false;
}
