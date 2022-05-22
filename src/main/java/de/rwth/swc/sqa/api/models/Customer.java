package de.rwth.swc.sqa.api.models;

import java.time.LocalDate;

public class Customer extends CustomerRequestBody {
    public long id;
    public Customer(long id, String birthdate, boolean disabled) throws Exception {
        try {
            LocalDate.parse(birthdate, dateFormatter);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        this.id = id;
        this.birthdate = birthdate;
        this.disabled = disabled;
    }
}
