package com.prometheus.findMetrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultObject {
    private List<String> existingAlgorithms;

    private List<String> nonExistingAlgorithms;
    Map<String, List<String>> algorithmsForDifferentPrefix;

    public ResultObject(List<String> existingAlgorithms, List<String> nonExistingAlgorithms, Map<String, List<String>> algorithmsForDifferentPrefix) {
        this.existingAlgorithms = existingAlgorithms;
        this.nonExistingAlgorithms = nonExistingAlgorithms;
        this.algorithmsForDifferentPrefix = algorithmsForDifferentPrefix;
    }
    public List<String> getAlgorithmNames() {
        return new ArrayList<>(algorithmsForDifferentPrefix.keySet());
    }
    public List<List<String>> getAlgorithmsWithPrefixValues() {
        return new ArrayList<>(algorithmsForDifferentPrefix.values());
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

    public Map<String, List<String>> getAlgorithmsForDifferentPrefix() {
        return algorithmsForDifferentPrefix;
    }

    public void setAlgorithmsForDifferentPrefix(Map<String, List<String>> algorithmsForDifferentPrefix) {
        this.algorithmsForDifferentPrefix = algorithmsForDifferentPrefix;
    }
}
