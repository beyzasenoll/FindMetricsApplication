package com.assia.metrics.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


public class ResultObject {
    private DifferentPrefixAlgorithmResult differentPrefixResult;
    private ExistingAlgorithmResult existingAlgorithmResult;
    private NonExistingAlgorithmResult nonExistingAlgorithmResult;
    private LocalDateTime dateTimeLocal;
    private OffsetDateTime dateTimeUTC;

    public ResultObject(ExistingAlgorithmResult existingAlgorithmResult, NonExistingAlgorithmResult nonExistingAlgorithmResult, DifferentPrefixAlgorithmResult differentPrefixResult) {
        this.existingAlgorithmResult = existingAlgorithmResult;
        this.nonExistingAlgorithmResult = nonExistingAlgorithmResult;
        this.differentPrefixResult = differentPrefixResult;
        this.dateTimeUTC = OffsetDateTime.now(ZoneOffset.UTC);
        this.dateTimeLocal = LocalDateTime.now();
    }

    public DifferentPrefixAlgorithmResult getDifferentPrefixResult() {
        return differentPrefixResult;
    }

    public ExistingAlgorithmResult getExistingAlgorithmResult() {
        return existingAlgorithmResult;
    }

    public NonExistingAlgorithmResult getNonExistingAlgorithmResult() {
        return nonExistingAlgorithmResult;
    }

    public String getDateTimeLocalFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateTimeLocal.format(formatter);
    }

    public String getDateTimeUTCFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateTimeUTC.format(formatter);
    }

}
