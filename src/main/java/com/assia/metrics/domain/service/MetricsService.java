package com.assia.metrics.domain.service;

import com.assia.metrics.domain.model.BaseObject;
import com.assia.metrics.domain.model.PrometheusMetricResponse;
import com.assia.metrics.domain.model.Result;
import com.assia.metrics.dto.ResultObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;

import java.io.*;
import java.lang.reflect.Field;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    @Value("${date.duration}")
    private int dateDuration;

    @Value("${step}")
    String step ;



    public MetricsService(WebClient.Builder webClientBuilder,
                          @Value("${prometheus.api.base-url}") String prometheusApiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(prometheusApiBaseUrl).build();
    }

    public ResultObject findAlgorithmMetrics() throws IOException {
        final Logger logger = LoggerFactory.getLogger(MetricsService.class);
        resetLogFile();
        List<String> existingAlgorithmsTimeStamps = new ArrayList<>();
        List<String> algorithmsForDifferentPrefixTimestamps = new ArrayList<>();


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
                    existingAlgorithmsTimeStamps.add(findLastFoundTimestampAsString("prometheus_http_response_size_bytes_count"));
                    break;
                } else if (metricResponse.contains(algorithm)) {
                    found = true;
                    algorithmsForDifferentPrefix.put(algorithm, metricResponse);
                    algorithmsForDifferentPrefixTimestamps.add(findLastFoundTimestampAsString("prometheus_http_response_size_bytes_count"));
                }
            }

            if (!found) {
                nonExistingAlgorithms.add(algorithm);
            }
        }
       //  Collections.sort(existingAlgorithms);
       // Collections.sort(nonExistingAlgorithms);

        ResultObject resultObject = new ResultObject(existingAlgorithms, nonExistingAlgorithms, algorithmsForDifferentPrefix,existingAlgorithmsTimeStamps,algorithmsForDifferentPrefixTimestamps);

        logger.info("Algorithm metrics search completed.");
        deleteAlgorithmClassFile();

        return resultObject;
    }

    //for only local test purposes
    private PrometheusMetricResponse readJsonDataFromFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(new File(metricResponseFilePath), PrometheusMetricResponse.class);
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
        File classFile = new File(classFilePath);

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
    public Instant findLastFoundTimestamp(String metric) {
        Instant endTimestamp = Instant.now();
        Instant startTimestamp = endTimestamp.minus(Duration.ofDays(dateDuration));

        BaseObject baseObject = createUrlAndFetchData(metric, startTimestamp, endTimestamp, step);

        if (baseObject == null || baseObject.getData().getResult() == null) {
            return null;
        }
        List<Result> results = baseObject.getData().getResult();
        Double maxLastValue = null;

        for (Result result : results) {
            List<List<Double>> values = result.getValues();
            if (values != null && !values.isEmpty()) {
                Double lastValue = values.get(values.size() - 1).get(0);
                if (maxLastValue == null || lastValue > maxLastValue) {
                    maxLastValue = lastValue;
                }
            }
        }
        if (maxLastValue != null) {
            long epochSeconds = maxLastValue.longValue();
            return Instant.ofEpochSecond(epochSeconds);
        }

        return null;
    }

    private BaseObject createUrlAndFetchData(String query, Instant startTimestamp, Instant endTimestamp, String step) {
        String apiUrl = "http://localhost:9090";
        String queryUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .path("/api/v1/query_range")
                .queryParam("query", query)
                .queryParam("start", startTimestamp.toString())
                .queryParam("end", endTimestamp.toString())
                .queryParam("step", step)
                .toUriString();
        return fetchDataFromApiUrl(queryUrl, BaseObject.class);
    }
    private PrometheusMetricResponse fetchDataFromApi() {
        return fetchDataFromApiUrl(searchFromPrometheusApi, PrometheusMetricResponse.class);
    }
    private <T> T fetchDataFromApiUrl(String url, Class<T> responseType) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }
    public String findLastFoundTimestampAsString(String metric) {
        Instant lastFoundInstant = findLastFoundTimestamp(metric);
        if (lastFoundInstant != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return lastFoundInstant.atZone(ZoneId.systemDefault()).format(formatter);
        }
        return null;
    }
}
