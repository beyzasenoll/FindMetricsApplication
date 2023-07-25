package com.prometheus.findMetrics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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


        ResultObject resultObject = metricsService.findMetricsAlgorithms();
        List<String> existingAlgorithms = resultObject.getExistingAlgorithms();
        List<String> nonExistingAlgorithms = resultObject.getNonExistingAlgorithms();
        Map<String, String> algorithmsForDifferentPrefix = resultObject.getAlgorithmsForDifferentPrefix();
        String dateTimeLocal = resultObject.getDateTimeLocal();
        String dateTimeUTC = resultObject.getDateTimeUTC();


        model.addAttribute("prefix", algorithmsPrefix);
        model.addAttribute("existingAlgorithms", existingAlgorithms);
        model.addAttribute("nonExistingAlgorithms", nonExistingAlgorithms);
        model.addAttribute("algorithmsForDifferentPrefix", algorithmsForDifferentPrefix);
        model.addAttribute("dateTimeLocal", dateTimeLocal);
        model.addAttribute("dateTimeUTC", dateTimeUTC);

        return "index";
    }
}
