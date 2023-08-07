package com.assia.metrics.dto;

import java.util.List;

public class NonExistingAlgorithmResult {
    private List<String> nonExistingAlgorithms;

    public NonExistingAlgorithmResult(List<String> nonExistingAlgorithms) {
        this.nonExistingAlgorithms = nonExistingAlgorithms;
    }

    public List<String> getNonExistingAlgorithms() {
        return nonExistingAlgorithms;
    }

    public void setNonExistingAlgorithms(List<String> nonExistingAlgorithms) {
        this.nonExistingAlgorithms = nonExistingAlgorithms;
    }
}
