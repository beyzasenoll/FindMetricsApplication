package com.prometheus.findMetrics;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import java.io.IOException;
import java.util.List;
@Controller
public class MetricsController {
    private final MetricsService metricsService;


    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }


    @GetMapping("/findMetrics")
    public String searchMetric(Model model) throws IOException {


        ResultObject resultObject = metricsService.findMetricsAlgorithms();
        List<String> existingAlgorithms=resultObject.getExistingAlgorithms();
        List<String> nonExistingAlgorithms=resultObject.getNonExistingAlgorithms();
        model.addAttribute("existingAlgorithms", existingAlgorithms);
        model.addAttribute("nonExistingAlgorithms", nonExistingAlgorithms);

        return "metrics_reports";
    }
}
