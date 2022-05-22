package de.rwth.swc.sqa.api.models;

public class DiscountCardRequestBody implements CustomDateTimeFormatter {
    public int type;
    public String validFrom;
    public String validFor;
}
