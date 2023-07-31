package com.assia.metrics.domain.service;

import com.assia.metrics.domain.model.PrometheusMetricResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.assia.metrics.dto.ResultObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

@Service
public class MetricsService {

    private final WebClient webClient;

    //for only local test purposes
    @Value("${metrics.response.filepath}")
    private String metricResponseFilePath;
    @Value("${prefix}")
    private String algorithmsPrefix;
    @Value("${prometheus.api.search.all.endpoint}")
    private String searchFromPrometheusApi;

    @Value("${external.class.filepath}")
    private String externalClassFilePath;

    @Value("${external.class.filename}")
    private String externalClassFileName;

    @Value("${read.from.file}")
    private boolean readFromFile;


    public MetricsService(WebClient.Builder webClientBuilder,
                          @Value("${prometheus.api.base-url}") String prometheusApiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(prometheusApiBaseUrl).build();
    }

    public ResultObject findAlgorithmMetrics() throws IOException {
        PrometheusMetricResponse prometheusMetricResponses;

        if(readFromFile){
            prometheusMetricResponses = readJsonDataFromFile();
        }
        else {
            prometheusMetricResponses = fetchDataFromApi();
        }

        List<String> existingAlgorithms = new ArrayList<>();
        List<String> nonExistingAlgorithms = new ArrayList<>();
        Map<String, String> algorithmsForDifferentPrefix = new HashMap<>();

        List<String> metricsFromPrometheus = prometheusMetricResponses.getData();
        List<String> algorithmList = readAlgorithmsFromExternalClass();

        for (String algorithm : algorithmList) {
            boolean found = false;
            for (String metricResponse : metricsFromPrometheus) {
                String prefixedMetric = algorithmsPrefix + algorithm;
                if (prefixedMetric.equals(metricResponse)) {
                    found = true;
                    existingAlgorithms.add(algorithm);
                    break;
                } else if (metricResponse.contains(algorithm)) {
                    found = true;
                    algorithmsForDifferentPrefix.put(algorithm, metricResponse);
                }
            }

            if (!found) {
                nonExistingAlgorithms.add(algorithm);
            }
        }
        Collections.sort(existingAlgorithms);
        Collections.sort(nonExistingAlgorithms);

        ResultObject resultObject = new ResultObject(existingAlgorithms, nonExistingAlgorithms, algorithmsForDifferentPrefix);
        return resultObject;
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
    public List<String> readAlgorithmsFromExternalClass() {
        List<String> algorithmList = new ArrayList<>();
        String fullFileName = externalClassFilePath+externalClassFileName+".java";
        deletePackage(fullFileName);
        boolean compilationSuccess = ExternalClassLoader.compileJavaFile(fullFileName);
        if (compilationSuccess) {
            Class<?> externalClass =  ExternalClassLoader.loadExternalClass(externalClassFilePath, externalClassFileName);

            for (Field declaredField : externalClass.getDeclaredFields()) {
                int modifiers = declaredField.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers)) {
                    algorithmList.add(declaredField.getName());
                }
            }
        } else {
            System.err.println("Compilation failed. The external class could not be loaded.");
        }

        return algorithmList;
    }
    public void deletePackage(String inputFilePath){

        try {
            File inputFile = new File(inputFilePath);
            String tempFile = inputFilePath + ".temp";
            File outputFile = new File(tempFile);
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            String line;
            while ((line = reader.readLine()) != null) {
                // Check if the line starts with "package" (ignore leading/trailing whitespace)
                if (line.trim().startsWith("package")) {
                    // Skip the line, do not write it to the output file
                    continue;
                }
                // Write the line to the output file
                writer.write(line);
                writer.newLine();
            }

            // Close the reader and writer
            reader.close();
            writer.close();

            // Replace the original file with the updated file (optional)
            if (inputFile.delete()) {
                outputFile.renameTo(inputFile);
            } else {
                System.out.println("Failed to delete the original file or rename the output file.");
            }

            System.out.println("Package lines have been deleted from the file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
