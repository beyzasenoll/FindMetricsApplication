package com.assia.metrics.controller;

import com.assia.metrics.domain.service.MetricsService;
import com.assia.metrics.dto.DifferentPrefixAlgorithmResult;
import com.assia.metrics.dto.ExistingAlgorithmResult;
import com.assia.metrics.dto.NonExistingAlgorithmResult;
import com.assia.metrics.dto.ResultObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @GetMapping({"/", "/findMetrics"})
    public String searchMetric(Model model) throws IOException {
        final Logger logger = LoggerFactory.getLogger(MetricsController.class);
        logger.info("Handling request to search metrics.");

        ResultObject resultObject = metricsService.findAlgorithmMetrics();

        ExistingAlgorithmResult existingAlgorithmResult = resultObject.getExistingAlgorithmResult();
        NonExistingAlgorithmResult nonExistingAlgorithmResult = resultObject.getNonExistingAlgorithmResult();
        DifferentPrefixAlgorithmResult differentPrefixAlgorithmResult = resultObject.getDifferentPrefixResult();

        model.addAttribute("prefix", algorithmsPrefix);
        model.addAttribute("dateTimeLocal", resultObject.getDateTimeLocalFormatted());
        model.addAttribute("dateTimeUTC", resultObject.getDateTimeUTCFormatted());
        model.addAttribute("existingAlgorithms", existingAlgorithmResult.getExistingAlgorithms());
        model.addAttribute("nonExistingAlgorithms", nonExistingAlgorithmResult.getNonExistingAlgorithms());
        model.addAttribute("algorithmsForDifferentPrefix", differentPrefixAlgorithmResult.getAlgorithmsForDifferentPrefix());
        model.addAttribute("existingAlgorithmsTimeStamps", existingAlgorithmResult.getExistingAlgorithmsTimeStamps());
        model.addAttribute("algorithmsForDifferentPrefixTimeStamps", differentPrefixAlgorithmResult.getAlgorithmsForDifferentPrefixTimestamps());
        model.addAttribute("existingAlgorithmsColors",existingAlgorithmResult.getExistingAlgorithmsColors());
        model.addAttribute("algorithmsForDifferentPrefixColors",differentPrefixAlgorithmResult.getAlgorithmsForDifferentPrefixColors());

        logger.info("Adding metrics data to the model.");
        return "index";
    }
}
