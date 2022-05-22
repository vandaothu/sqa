package de.rwth.swc.sqa.api.models;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public interface CustomDateTimeFormatter {
    DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-[MM][M]-[dd][d]"))
            .toFormatter();
    DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendOptional(DateTimeFormatter.ofPattern("yyyy-[MM][M]-[dd][d]'T'[HH][H]:[mm][m]:[ss][s]"))
            .toFormatter();

}
