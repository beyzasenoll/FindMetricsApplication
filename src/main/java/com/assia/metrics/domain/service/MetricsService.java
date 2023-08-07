package com.assia.metrics.domain.service;

import com.assia.metrics.domain.model.PrometheusMetricResponseTimestamp;
import com.assia.metrics.domain.model.PrometheusMetricResponse;
import com.assia.metrics.domain.model.Result;
import com.assia.metrics.dto.DifferentPrefixAlgorithmResult;
import com.assia.metrics.dto.ExistingAlgorithmResult;
import com.assia.metrics.dto.NonExistingAlgorithmResult;
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
import java.time.temporal.ChronoUnit;
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

        PrometheusMetricResponse prometheusMetricResponses;

        if (readFromFile) {
            logger.debug("Reading metrics data from file: {}", metricResponseFilePath);
            prometheusMetricResponses = readJsonDataFromFile();
        } else {
            logger.debug("Fetching metrics data from Prometheus API: {}", searchFromPrometheusApi);
            prometheusMetricResponses = fetchDataFromApi();
        }

        List<String> metricsFromPrometheus = prometheusMetricResponses.getData();
        List<String> algorithmList = readAlgorithmsFromExternalClass();

        DifferentPrefixAlgorithmResult differentPrefixResult = new DifferentPrefixAlgorithmResult(new HashMap<>(), new ArrayList<>(),new ArrayList<>());
        ExistingAlgorithmResult existingAlgorithmResult = new ExistingAlgorithmResult(new ArrayList<>(), new ArrayList<>(),new ArrayList<>());
        NonExistingAlgorithmResult nonExistingAlgorithmResult = new NonExistingAlgorithmResult(new ArrayList<>());

        for (String algorithm : algorithmList) {
            boolean found = false;
            String color = "white";
            for (String metricResponse : metricsFromPrometheus) {
                String prefixedMetric = algorithmsPrefix + algorithm;
                if (prefixedMetric.equals(metricResponse)) {
                    found = true;
                    existingAlgorithmResult.getExistingAlgorithms().add(algorithm);
                    existingAlgorithmResult.getExistingAlgorithmsTimeStamps().add(findLastFoundTimestampAsString("prometheus_http_response_size_bytes_count")); //prefixedMetric
                    color=getColorForTimestamp(findLastFoundTimestamp("prometheus_http_response_size_bytes_count"));
                    existingAlgorithmResult.getExistingAlgorithmsColors().add(color);
                    break;
                } else if (metricResponse.contains(algorithm)) {
                    found = true;
                    differentPrefixResult.getAlgorithmsForDifferentPrefix().put(algorithm, metricResponse);
                    differentPrefixResult.getAlgorithmsForDifferentPrefixTimestamps().add(findLastFoundTimestampAsString("prometheus_http_response_size_bytes_count"));//metricResponse
                    color=getColorForTimestamp(findLastFoundTimestamp("prometheus_http_response_size_bytes_count"));
                    differentPrefixResult.getAlgorithmsForDifferentPrefixColors().add(color);
                }
            }

            if (!found) {
                nonExistingAlgorithmResult.getNonExistingAlgorithms().add(algorithm);
            }
        }

        ResultObject resultObject = new ResultObject(existingAlgorithmResult, nonExistingAlgorithmResult, differentPrefixResult);

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

        PrometheusMetricResponseTimestamp prometheusMetricResponseTimestamp = createUrlAndFetchData(metric, startTimestamp, endTimestamp, step);

        if (prometheusMetricResponseTimestamp == null || prometheusMetricResponseTimestamp.getData().getResult() == null) {
            return null;
        }
        List<Result> results = prometheusMetricResponseTimestamp.getData().getResult();
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
    public String findLastFoundTimestampAsString(String metric) {
        Instant lastFoundInstant = findLastFoundTimestamp(metric);
        if (lastFoundInstant != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return lastFoundInstant.atZone(ZoneId.systemDefault()).format(formatter);
        }
        return null;
    }

    private PrometheusMetricResponseTimestamp createUrlAndFetchData(String query, Instant startTimestamp, Instant endTimestamp, String step) {
        String apiUrl = "http://localhost:9090";
        String queryUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .path("/api/v1/query_range")
                .queryParam("query", query)
                .queryParam("start", startTimestamp.toString())
                .queryParam("end", endTimestamp.toString())
                .queryParam("step", step)
                .toUriString();
        return fetchDataFromApiUrl(queryUrl, PrometheusMetricResponseTimestamp.class);
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

    private String getColorForTimestamp(Instant timestamp) {
        if (timestamp != null) {
            Instant now = Instant.now();
            Instant oneDayAgo = now.minus(Duration.ofDays(1));
            Instant sixHoursAgo = now.minus(Duration.ofHours(6));

            if (timestamp.isBefore(oneDayAgo)) {
                return "red";
            } else if (timestamp.isBefore(sixHoursAgo) ) {
                return "yellow";
            } else if (timestamp.truncatedTo(ChronoUnit.MINUTES).equals(now.truncatedTo(ChronoUnit.MINUTES))) {
                return "green";
            }
        }

        return "white";
    }

}
