package com.prometheus.findMetrics;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ResultObject {
    Map<String, String> algorithmsForDifferentPrefix;
    LocalDateTime dateTimeLocal;
    OffsetDateTime dateTimeUTC;
    private List<String> existingAlgorithms;
    private List<String> nonExistingAlgorithms;


    public ResultObject(List<String> existingAlgorithms, List<String> nonExistingAlgorithms, Map<String, String> algorithmsForDifferentPrefix) {
        this.existingAlgorithms = existingAlgorithms;
        this.nonExistingAlgorithms = nonExistingAlgorithms;
        this.algorithmsForDifferentPrefix = algorithmsForDifferentPrefix;
        this.dateTimeUTC = OffsetDateTime.now(ZoneOffset.UTC);
        this.dateTimeLocal = LocalDateTime.now();
    }


    public List<String> getExistingAlgorithms() {
        return existingAlgorithms;
    }

    public void setExistingAlgorithms(List<String> existingAlgorithms) {
        this.existingAlgorithms = existingAlgorithms;
    }

    public List<String> getNonExistingAlgorithms() {
        return nonExistingAlgorithms;
    }

    public void setNonExistingAlgorithms(List<String> nonExistingAlgorithms) {
        this.nonExistingAlgorithms = nonExistingAlgorithms;
    }

    public Map<String, String> getAlgorithmsForDifferentPrefix() {
        return algorithmsForDifferentPrefix;
    }

    public void setAlgorithmsForDifferentPrefix(Map<String, String> algorithmsForDifferentPrefix) {
        this.algorithmsForDifferentPrefix = algorithmsForDifferentPrefix;
    }

    public String getDateTimeLocal() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateTimeLocal.format(formatter);
    }

    public void setDateTimeLocal(LocalDateTime dateTimeLocal) {
        this.dateTimeLocal = dateTimeLocal;
    }

    public String getDateTimeUTC() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss Z");
        return dateTimeUTC.format(formatter);
    }

    public void setDateTimeUTC(OffsetDateTime dateTimeUTC) {
        this.dateTimeUTC = dateTimeUTC;
    }


}
