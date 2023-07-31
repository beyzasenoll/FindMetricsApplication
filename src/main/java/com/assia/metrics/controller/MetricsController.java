package com.assia.metrics.controller;

import com.assia.metrics.dto.ResultObject;
import com.assia.metrics.domain.service.MetricsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class MetricsController {
    private final MetricsService metricsService;
    @Value("${prefix}")
    private String algorithmsPrefix;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/findMetrics")
    public String searchMetric(Model model) throws IOException {

        ResultObject resultObject = metricsService.findAlgorithmMetrics();

        model.addAttribute("prefix", algorithmsPrefix);
        model.addAttribute("existingAlgorithms", resultObject.getExistingAlgorithms());
        model.addAttribute("nonExistingAlgorithms", resultObject.getNonExistingAlgorithms());
        model.addAttribute("algorithmsForDifferentPrefix", resultObject.getAlgorithmsForDifferentPrefix());
        model.addAttribute("dateTimeLocal", resultObject.getDateTimeLocalFormatted());
        model.addAttribute("dateTimeUTC", resultObject.getDateTimeUTCFormatted());

        return "index";
    }
}
