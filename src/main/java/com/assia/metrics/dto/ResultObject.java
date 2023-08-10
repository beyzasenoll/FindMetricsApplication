package com.assia.metrics.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


public class ResultObject {
    private List<String> nonExistingAlgorithms;
    private List<Map<String, String>> existingAlgorithmsList;
    private List<Map<String, String>> differentPrefixAlgorithmsList;
    private LocalDateTime dateTimeLocal;
    private OffsetDateTime dateTimeUTC;

    public ResultObject(List<String> nonExistingAlgorithms, List<Map<String, String>> existingAlgorithmsList, List<Map<String, String>> differentPrefixAlgorithmsList) {
        this.nonExistingAlgorithms = nonExistingAlgorithms;
        this.existingAlgorithmsList = existingAlgorithmsList;
        this.differentPrefixAlgorithmsList = differentPrefixAlgorithmsList;
        this.dateTimeUTC = OffsetDateTime.now(ZoneOffset.UTC);
        this.dateTimeLocal = LocalDateTime.now();

    }

    public List<Map<String, String>> getExistingAlgorithmsList() {
        return existingAlgorithmsList;
    }

    public void setExistingAlgorithmsList(List<Map<String, String>> existingAlgorithmsList) {
        this.existingAlgorithmsList = existingAlgorithmsList;
    }

    public List<Map<String, String>> getDifferentPrefixAlgorithmsList() {
        return differentPrefixAlgorithmsList;
    }

    public void setDifferentPrefixAlgorithmsList(List<Map<String, String>> differentPrefixAlgorithmsList) {
        this.differentPrefixAlgorithmsList = differentPrefixAlgorithmsList;
    }

    public List<String> getNonExistingAlgorithms() {
        return nonExistingAlgorithms;
    }

    public void setNonExistingAlgorithms(List<String> nonExistingAlgorithms) {
        this.nonExistingAlgorithms = nonExistingAlgorithms;
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
