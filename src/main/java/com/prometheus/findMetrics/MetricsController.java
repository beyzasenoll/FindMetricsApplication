package com.prometheus.findMetrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import java.io.IOException;
import java.util.*;

@Controller
public class MetricsController {
    private final MetricsService metricsService;


    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Value("${prefix}")
    private String algorithmsPrefix;

    @GetMapping("/findMetrics")
    public String searchMetric(Model model) throws IOException {


        ResultObject resultObject = metricsService.findMetricsAlgorithms();
        List<String> existingAlgorithms=resultObject.getExistingAlgorithms();
        List<String> nonExistingAlgorithms=resultObject.getNonExistingAlgorithms();
        Map<String, String> algorithmsForDifferentPrefix=resultObject.getAlgorithmsForDifferentPrefix();

        model.addAttribute("prefix",algorithmsPrefix);
        model.addAttribute("existingAlgorithms", existingAlgorithms);
        model.addAttribute("nonExistingAlgorithms", nonExistingAlgorithms);
        model.addAttribute("algorithmsForDifferentPrefix",algorithmsForDifferentPrefix);

        return "index";
    }
}
