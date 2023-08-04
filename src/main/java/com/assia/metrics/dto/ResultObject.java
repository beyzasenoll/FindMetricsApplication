package com.assia.metrics.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResultObject {
    private Map<String, String> algorithmsForDifferentPrefix;
    private LocalDateTime dateTimeLocal;
    private OffsetDateTime dateTimeUTC;
    private List<String> existingAlgorithms;
    private List<String> nonExistingAlgorithms;
    private List<String> algorithmsForDifferentPrefixTimestamps;
    private List<String> existingAlgorithmsTimeStamps;



    public ResultObject(List<String> existingAlgorithms, List<String> nonExistingAlgorithms, Map<String, String> algorithmsForDifferentPrefix, List<String> existingAlgorithmsWithTimeStamps, List<String> algorithmsForDifferentPrefixWithTimestamps) {
        this.existingAlgorithms = existingAlgorithms;
        this.nonExistingAlgorithms = nonExistingAlgorithms;
        this.algorithmsForDifferentPrefix = algorithmsForDifferentPrefix;
        this.dateTimeUTC = OffsetDateTime.now(ZoneOffset.UTC);
        this.dateTimeLocal = LocalDateTime.now();
        this.existingAlgorithmsTimeStamps=existingAlgorithmsWithTimeStamps;
        this.algorithmsForDifferentPrefixTimestamps=algorithmsForDifferentPrefixWithTimestamps;
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

    public String getDateTimeLocalFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateTimeLocal.format(formatter);
    }


    public void setAlgorithmsForDifferentPrefixTimestamps(List<String> algorithmsForDifferentPrefixTimestamps) {
        this.algorithmsForDifferentPrefixTimestamps = algorithmsForDifferentPrefixTimestamps;
    }

    public List<String> getExistingAlgorithmsTimeStamps() {
        return existingAlgorithmsTimeStamps;
    }

    public void setExistingAlgorithmsTimeStamps(List<String> existingAlgorithmsTimeStamps) {
        this.existingAlgorithmsTimeStamps = existingAlgorithmsTimeStamps;
    }
    public String getDateTimeUTCFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateTimeUTC.format(formatter);
    }
    public List<String> getAlgorithmsForDifferentPrefixTimestamps() {
        return algorithmsForDifferentPrefixTimestamps;
    }
}
