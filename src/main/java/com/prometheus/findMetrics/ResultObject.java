package com.prometheus.findMetrics;

import java.util.List;

public class ResultObject {
    private List<String> existingAlgorithms;

    private List<String> nonExistingAlgorithms;

    public ResultObject(List<String> existingAlgorithms, List<String> nonExistingAlgorithms) {
        this.existingAlgorithms = existingAlgorithms;
        this.nonExistingAlgorithms = nonExistingAlgorithms;
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
}
