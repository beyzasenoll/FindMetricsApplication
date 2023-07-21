package com.prometheus.findMetrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MetricListService {

    private final WebClient webClient;

    @Value("${file.path.response}")
    private String responseFilePath;
    @Value("${file.path.metrics}")
    private String algorithmsFilePath;
    @Value("${prefix}")
    private String algorithmsPrefix;
    @Value("${api.endpoint}")
    private String apiEndpoint;



    public MetricListService(WebClient.Builder webClientBuilder,
                             @Value("${prometheus.api.base-url}") String PrometheusApiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(PrometheusApiBaseUrl).build();
    }

    public List<String> findMetricsAlgorithms() throws IOException {
        MetricResponse metricResponses = fetchDataFromApi();
        List<String> existingAlgorithms = new ArrayList<>();
        List<String> nonExistingAlgorithms = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();
        MetricAlgorithms algorithms = objectMapper.readValue(new File(algorithmsFilePath), MetricAlgorithms.class);

        List<String> metricResponseList = metricResponses.getData();
        List<String> algorithmList = algorithms.getData();

        for (String algorithm : algorithmList) {
            boolean found = false;
            for (String metricResponse : metricResponseList) {
                String prefixedMetric = algorithmsPrefix + algorithm;
                if (prefixedMetric.equals(metricResponse)) {
                    found = true;
                    existingAlgorithms.add(algorithm);
                    break;
                }
            }
            if (!found) {
                nonExistingAlgorithms.add(algorithm);
            }
        }

        return Arrays.asList(
                "Existing Metrics:", existingAlgorithms.toString(), "", "Non-Existing Metrics:", nonExistingAlgorithms.toString());
    }

    private MetricResponse readJsonDataFromFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(responseFilePath), MetricResponse.class);
    }

    private MetricResponse fetchDataFromApi() {
        MetricResponse baseObject = webClient.get()
                .uri(apiEndpoint)
                .retrieve()
                .bodyToMono(MetricResponse.class)
                .block();
        return baseObject;
    }
}
