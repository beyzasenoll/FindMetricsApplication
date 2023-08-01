package com.assia.metrics.domain.service;

import com.assia.metrics.domain.model.PrometheusMetricResponse;
import com.assia.metrics.dto.ResultObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    @Value("${logging.file.name}")
    private String loggingFileName;


    public MetricsService(WebClient.Builder webClientBuilder,
                          @Value("${prometheus.api.base-url}") String prometheusApiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(prometheusApiBaseUrl).build();
    }

    public ResultObject findAlgorithmMetrics() throws IOException {
        final Logger logger = LoggerFactory.getLogger(MetricsService.class);
        resetLogFile();

        PrometheusMetricResponse prometheusMetricResponses;

        if (readFromFile) {
            logger.debug("Reading metrics data from file: {}", metricResponseFilePath);
            prometheusMetricResponses = readJsonDataFromFile();
        } else {
            logger.debug("Fetching metrics data from Prometheus API: {}", searchFromPrometheusApi);
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

        logger.info("Algorithm metrics search completed.");
        deleteAlgorithmClassFile();

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
        final Logger logger = LoggerFactory.getLogger(MetricsService.class);
        logger.info("Reading algorithms from external class.");

        List<String> algorithmList = new ArrayList<>();
        String fullFileName = externalClassFilePath + externalClassFileName + ".java";
        deletePackage(fullFileName);
        boolean compilationSuccess = ExternalClassLoader.compileJavaFile(fullFileName);
        if (compilationSuccess) {
            Class<?> externalClass = ExternalClassLoader.loadExternalClass(externalClassFilePath, externalClassFileName);

            for (Field declaredField : externalClass.getDeclaredFields()) {
                int modifiers = declaredField.getModifiers();
                if (java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers)) {
                    algorithmList.add(declaredField.getName());
                }
            }
            logger.info("Successfully read algorithms from the external class.");
        } else {
            logger.error("Compilation failed. The external class could not be loaded.");
        }
        return algorithmList;
    }

    public void deletePackage(String inputFilePath) {
        final Logger logger = LoggerFactory.getLogger(MetricsService.class);
        try {
            logger.debug("Deleting package statements from the file: {}", inputFilePath);
            File inputFile = new File(inputFilePath);
            String tempFile = inputFilePath + ".temp";
            File outputFile = new File(tempFile);
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("package")) {
                    continue;
                }
                writer.write(line);
                writer.newLine();
            }
            reader.close();
            writer.close();

            if (inputFile.delete()) {
                outputFile.renameTo(inputFile);
            } else {
                logger.error("Failed to delete the original file or rename the output file.");
            }
            logger.info("Package lines have been deleted from the file.");
        } catch (IOException e) {
            logger.error("Error while deleting package statements from the file: {}", inputFilePath);
            e.printStackTrace();
        }
    }

    public void deleteAlgorithmClassFile() {
        final Logger logger = LoggerFactory.getLogger(MetricsService.class);
        String classFilePath = externalClassFilePath + externalClassFileName + ".class";
        File classFile = new File(clasilePath);

        if (classFile.exists()) {
            if (classFile.delete()) {
                logger.info("AlgorithmNames.class file has been deleted.");
            } else {
                logger.error("Failed to delete AlgorithmNames.class file.");
            }
        } else {
            logger.warn("AlgorithmNames.class file does not exist.");
        }
    }

    private void resetLogFile() {
        final Logger logger = LoggerFactory.getLogger(MetricsService.class);
        String logFileName = loggingFileName;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, false))) {
        } catch (IOException e) {
            logger.error("Error while resetting log file: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}
