package com.prometheus.findMetrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
@RestController
public class MetricResponseController {
    private final MetricListService baseObjectService;


    public MetricResponseController(MetricListService baseObjectService) {
        this.baseObjectService = baseObjectService;
    }

    @GetMapping("/findMetrics")
    public List<String> searchMetric() throws IOException {
        return baseObjectService.findMetricsAlgorithms();
    }
}
