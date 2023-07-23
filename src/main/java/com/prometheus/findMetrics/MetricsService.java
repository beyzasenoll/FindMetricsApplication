package com.prometheus.findMetrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class MetricsService {

    private final WebClient webClient;

    //for only local test purposes
    @Value("${matrics.response.filepath}")
    private String metricResponseFilePath;
    @Value("${file.path.metrics}")
    private String algorithmsFilePath;
    @Value("${prefix}")
    private String algorithmsPrefix;
    @Value("${search.all.api.endpoint}")
    private String searchFromPrometheusApi;



    public MetricsService(WebClient.Builder webClientBuilder,
                          @Value("${prometheus.api.base-url}") String prometheusApiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(prometheusApiBaseUrl).build();
    }

    public ResultObject findMetricsAlgorithms() throws IOException {
       // PrometheusMetricResponse prometheusMetricResponses = fetchDataFromApi();
        PrometheusMetricResponse prometheusMetricResponses = readJsonDataFromFile();


        List<String> existingAlgorithms = new ArrayList<>();
        List<String> nonExistingAlgorithms = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();

        List<String> metricsFromPrometheus = prometheusMetricResponses.getData();
        List<String> algorithmList = readAlgorithmsFromFile(algorithmsFilePath);


        for (String algorithm : algorithmList) {
            boolean found = false;
            for (String metricResponse : metricsFromPrometheus) {
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

        ResultObject resultObject = new ResultObject(existingAlgorithms, nonExistingAlgorithms);
        return resultObject;
    }

    public static List<String> readAlgorithmsFromFile(String filePath) {
        List<String> algorithms = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                algorithms.add(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return algorithms;
    }

   //for only local test purposes
    private PrometheusMetricResponse readJsonDataFromFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(metricResponseFilePath), PrometheusMetricResponse.class);
    }

    private PrometheusMetricResponse fetchDataFromApi() {
        PrometheusMetricResponse baseObject = webClient.get()
                .uri(searchFromPrometheusApi)
                .retrieve()
                .bodyToMono(PrometheusMetricResponse.class)
                .block();
        return baseObject;
    }
}
