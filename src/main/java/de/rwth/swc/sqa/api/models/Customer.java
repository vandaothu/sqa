package de.rwth.swc.sqa.api.models;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Customer extends CustomerRequestBody {
    public long id;
    public Customer(long id, String birthdate, boolean disabled) throws Exception {
        try {
            LocalDate bd = LocalDate.parse(birthdate, dateFormatter);
            LocalDate td = LocalDate.now();
            if (ChronoUnit.DAYS.between(bd, td) < 0) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception();
        }

        this.id = id;
        this.birthdate = birthdate;
        this.disabled = disabled;
    }
}
