package com.assia.metrics.dto;

import java.util.List;
import java.util.Map;

public class DifferentPrefixAlgorithmResult {
    private Map<String, String> algorithmsForDifferentPrefix;
    private List<String> algorithmsForDifferentPrefixTimestamps;
    private List<String> algorithmsForDifferentPrefixColors;
    public DifferentPrefixAlgorithmResult(Map<String, String> algorithmsForDifferentPrefix, List<String> algorithmsForDifferentPrefixTimestamps,List<String> algorithmsForDifferentPrefixColors) {
        this.algorithmsForDifferentPrefix = algorithmsForDifferentPrefix;
        this.algorithmsForDifferentPrefixTimestamps = algorithmsForDifferentPrefixTimestamps;
        this.algorithmsForDifferentPrefixColors=algorithmsForDifferentPrefixColors;
    }

    public Map<String, String> getAlgorithmsForDifferentPrefix() {
        return algorithmsForDifferentPrefix;
    }

    public void setAlgorithmsForDifferentPrefix(Map<String, String> algorithmsForDifferentPrefix) {
        this.algorithmsForDifferentPrefix = algorithmsForDifferentPrefix;
    }

    public List<String> getAlgorithmsForDifferentPrefixTimestamps() {
        return algorithmsForDifferentPrefixTimestamps;
    }

    public void setAlgorithmsForDifferentPrefixTimestamps(List<String> algorithmsForDifferentPrefixTimestamps) {
        this.algorithmsForDifferentPrefixTimestamps = algorithmsForDifferentPrefixTimestamps;
    }

    public List<String> getAlgorithmsForDifferentPrefixColors() {
        return algorithmsForDifferentPrefixColors;
    }

    public void setAlgorithmsForDifferentPrefixColors(List<String> algorithmsForDifferentPrefixColors) {
        this.algorithmsForDifferentPrefixColors = algorithmsForDifferentPrefixColors;
    }
}

