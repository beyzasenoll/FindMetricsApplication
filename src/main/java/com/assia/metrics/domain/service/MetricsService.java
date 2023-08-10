package com.assia.metrics.domain.service;

import com.assia.metrics.domain.model.PrometheusMetricResponse;
import com.assia.metrics.domain.model.PrometheusMetricResponseTimestamp;
import com.assia.metrics.domain.model.Result;
import com.assia.metrics.dto.ResultObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.*;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Value("${read.from.file:false}")
    private boolean readFromFile;
    @Value("${logging.file.name}")
    private String loggingFileName;

    @Value("${date.duration}")
    private int dateDuration;

    @Value("${step}")
    String step;
    @Value("${timestamp.color.before.one.day}")
    private String redColor;

    @Value("${timestamp.color.before.six.hours}")
    private String yellowColor;

    @Value("${timestamp.color.same.time}")
    private String greenColor;

    @Value("${timestamp.color.default}")
    private String whiteColor;


    public MetricsService(WebClient.Builder webClientBuilder,
                          @Value("${prometheus.api.base-url}") String prometheusApiBaseUrl) {
        this.webClient = webClientBuilder.baseUrl(prometheusApiBaseUrl).build();
    }

    public ResultObject findAlgorithmMetrics() throws IOException {
        final Logger logger = LoggerFactory.getLogger(MetricsService.class);
        resetLogFile();

        PrometheusMetricResponse prometheusMetricResponses;

        logger.debug("Fetching metrics data from Prometheus API: {}", searchFromPrometheusApi);
        prometheusMetricResponses = fetchDataFromApi();

        List<String> metricsFromPrometheus = prometheusMetricResponses.getData();
        List<String> algorithmList = readAlgorithmsFromExternalClass();

        List<Map<String, String>> existingAlgorithmsList = new ArrayList<>();
        List<Map<String, String>> differentPrefixAlgorithmsList = new ArrayList<>();
        List<String> nonExistingAlgorithms = new ArrayList<>();


        for (String algorithm : algorithmList) {
            boolean found = false;
            String color = "white";
            String timestampAsString;
            Instant timestamp;
            for (String metricResponse : metricsFromPrometheus) {
                String prefixedMetric = algorithmsPrefix + algorithm;

                Map<String, String> algorithmData = new HashMap<>();
                if (prefixedMetric.equals(metricResponse)) {
                    found = true;
                    timestamp = findLastFoundTimestamp(prefixedMetric);
                    timestampAsString = findLastFoundTimestampAsString(timestamp);
                    color = getColorForTimestamp(timestamp);

                    algorithmData.put("algorithm", algorithm);
                    algorithmData.put("prefixedMetric", prefixedMetric);
                    algorithmData.put("timestamp", timestampAsString);
                    algorithmData.put("color", color);

                    existingAlgorithmsList.add(algorithmData);
                    break;

                } else if (metricResponse.contains(algorithm)) {
                    found = true;
                    timestamp = findLastFoundTimestamp(metricResponse);
                    timestampAsString = findLastFoundTimestampAsString(timestamp);
                    color = getColorForTimestamp(timestamp);

                    algorithmData.put("algorithm", algorithm);
                    algorithmData.put("metricResponse", metricResponse);
                    algorithmData.put("timestamp", timestampAsString);
                    algorithmData.put("color", color);

                    differentPrefixAlgorithmsList.add(algorithmData);

                }
            }
            if (!found) {
                nonExistingAlgorithms.add(algorithm);
            }
        }

        ResultObject resultObject = new ResultObject(nonExistingAlgorithms, existingAlgorithmsList, differentPrefixAlgorithmsList);

        logger.info("Algorithm metrics search completed.");
        deleteAlgorithmClassFile();

        return resultObject;
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
        Instant startTimestamp = endTimestamp.minus(Duration.ofDays(1));

        while (startTimestamp.isBefore(endTimestamp) && Duration.between(startTimestamp, Instant.now()).toDays() <= dateDuration) {
            PrometheusMetricResponseTimestamp prometheusMetricResponseTimestamp = createUrlAndFetchData(metric, startTimestamp, endTimestamp, step);

            if (prometheusMetricResponseTimestamp != null && prometheusMetricResponseTimestamp.getData().getResult() != null) {
                Instant maxLastValueTimestamp = getMaxLastValueTimestamp(prometheusMetricResponseTimestamp);
                if (maxLastValueTimestamp != null) {
                    return maxLastValueTimestamp;
                }
            }
            startTimestamp = startTimestamp.minus(Duration.ofDays(1));
            endTimestamp = endTimestamp.minus(Duration.ofDays(1));
        }

        return null;
    }

    private Instant getMaxLastValueTimestamp(PrometheusMetricResponseTimestamp response) {
        List<Result> results = response.getData().getResult();
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

    public String findLastFoundTimestampAsString(Instant lastFoundInstant) {
        if (lastFoundInstant != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneOffset.UTC);
            return formatter.format(lastFoundInstant);
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
                return redColor;
            } else if (timestamp.isBefore(sixHoursAgo)) {
                return yellowColor;
            } else if (timestamp.truncatedTo(ChronoUnit.MINUTES).equals(now.truncatedTo(ChronoUnit.MINUTES))) {
                return greenColor;
            }
        }

        return whiteColor;
    }

}
