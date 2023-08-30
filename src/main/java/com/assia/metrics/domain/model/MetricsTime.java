package com.assia.metrics.domain.model;

import lombok.Data;

import java.time.Instant;

@Data
public class MetricsTime {
    public String type;
    public Instant value;
}
