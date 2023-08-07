package com.assia.metrics.dto;

import java.util.List;

public class ExistingAlgorithmResult {
    private List<String> existingAlgorithms;
    private List<String> existingAlgorithmsTimeStamps;
    private List<String> existingAlgorithmsColors;

    public ExistingAlgorithmResult(List<String> existingAlgorithms, List<String> existingAlgorithmsTimeStamps, List<String> existingAlgorithmsColors) {
        this.existingAlgorithms = existingAlgorithms;
        this.existingAlgorithmsTimeStamps = existingAlgorithmsTimeStamps;
        this.existingAlgorithmsColors = existingAlgorithmsColors;
    }

    public List<String> getExistingAlgorithms() {
        return existingAlgorithms;
    }

    public void setExistingAlgorithms(List<String> existingAlgorithms) {
        this.existingAlgorithms = existingAlgorithms;
    }

    public List<String> getExistingAlgorithmsTimeStamps() {
        return existingAlgorithmsTimeStamps;
    }

    public void setExistingAlgorithmsTimeStamps(List<String> existingAlgorithmsTimeStamps) {
        this.existingAlgorithmsTimeStamps = existingAlgorithmsTimeStamps;
    }

    public List<String> getExistingAlgorithmsColors() {
        return existingAlgorithmsColors;
    }

    public void setExistingAlgorithmsColors(List<String> existingAlgorithmsColors) {
        this.existingAlgorithmsColors = existingAlgorithmsColors;
    }
}

