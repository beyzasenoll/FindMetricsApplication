package com.assia.metrics.domain.service;

import com.assia.metrics.domain.model.MetricsTime;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


@Service
public class MetricsService {

    private final WebClient webClient;
    final Logger logger = LoggerFactory.getLogger(MetricsService.class);


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
    int step;
    @Value("${control.range}")
    private int controlRange;
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
        resetLogFile();

        PrometheusMetricResponse prometheusMetricResponses;

        logger.debug("Fetching metrics data from Prometheus API: {}", searchFromPrometheusApi);
        prometheusMetricResponses = fetchDataFromApi();

        List<String> metricsFromPrometheus = prometheusMetricResponses.getData();
        List<String> algorithmList = readAlgorithmsFromExternalClass();

        List<Map<String, String>> existingAlgorithmsList = new ArrayList<>();
        List<Map<String, String>> differentPrefixAlgorithmsList = new ArrayList<>();
        List<String> nonExistingAlgorithms = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<?>> futures = new ArrayList<>();

        for (String algorithm : algorithmList) {
            futures.add(executorService.submit(() -> {
                boolean found = false;
                String color = "white";
                String timestampUTC;
                String timestampLocal;
                Instant timestamp;

                for (String metricResponse : metricsFromPrometheus) {
                    String prefixedMetric = algorithmsPrefix + algorithm;

                    Map<String, String> algorithmData = new HashMap<>();
                    if (prefixedMetric.equals(metricResponse)) {
                        found = true;
                        timestamp = findLastFoundTimestamp(prefixedMetric);
                        timestampUTC = findLastFoundTimestampAsString(timestamp, ZoneOffset.UTC);
                        timestampLocal = findLastFoundTimestampAsString(timestamp, ZoneId.of("Europe/Istanbul"));
                        color = getColorForTimestamp(timestamp);


                        algorithmData.put("algorithm", algorithm);
                        algorithmData.put("prefixedMetric", prefixedMetric);
                        algorithmData.put("timestampUTC", timestampUTC);
                        algorithmData.put("timestampLocal", timestampLocal);
                        algorithmData.put("color", color);

                        existingAlgorithmsList.add(algorithmData);
                        break;

                    } else if (metricResponse.contains(algorithm)) {
                        found = true;
                        timestamp = findLastFoundTimestamp(metricResponse);
                        timestampUTC = findLastFoundTimestampAsString(timestamp, ZoneOffset.UTC);
                        timestampLocal = findLastFoundTimestampAsString(timestamp, ZoneId.of("Europe/Istanbul"));
                        color = getColorForTimestamp(timestamp);

                        algorithmData.put("algorithm", algorithm);
                        algorithmData.put("metricResponse", metricResponse);
                        algorithmData.put("timestampUTC", timestampUTC);
                        algorithmData.put("timestampLocal", timestampLocal);
                        algorithmData.put("color", color);
                        differentPrefixAlgorithmsList.add(algorithmData);
                    }
                }
                if (!found) {
                    nonExistingAlgorithms.add(algorithm);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                logger.error("Error executing task: {}", e.getMessage());
            }
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ResultObject resultObject = new ResultObject(nonExistingAlgorithms, existingAlgorithmsList, differentPrefixAlgorithmsList);

        logger.info("Algorithm metrics search completed.");
        deleteAlgorithmClassFile();

        return resultObject;
    }


    public List<String> readAlgorithmsFromExternalClass() {
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
        String logFileName = loggingFileName;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFileName, false))) {
        } catch (IOException e) {
            logger.error("Error while resetting log file: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public Instant findLastFoundTimestamp(String metric) {
        logger.info("Finding last found timestamp for metric: {}", metric);

        Instant currentInstant = Instant.now();
        Instant startInstant = currentInstant.minus(controlRange, ChronoUnit.MINUTES);

        long endTimestampSeconds = currentInstant.getEpochSecond();
        long startTimestampSeconds = startInstant.getEpochSecond();
        final int SECONDS_IN_DAY = 86400;

        while (startTimestampSeconds < endTimestampSeconds && currentInstant.getEpochSecond() - startTimestampSeconds <= dateDuration * SECONDS_IN_DAY) {
            PrometheusMetricResponseTimestamp prometheusMetricResponseTimestamp = createUrlAndFetchData(metric, startTimestampSeconds, endTimestampSeconds, step);

            MetricsTime metricsTime = getLastPeakTimestamp(prometheusMetricResponseTimestamp);

            if (metricsTime != null && metricsTime.value != null) {
                if (metricsTime.time.equals("OneHourMetric") || metricsTime.time.equals("CurrentMetric")) {
                    logger.info("Returning max last value timestamp for metric: {}", metric);
                    return metricsTime.value;
                }
            }

            startTimestampSeconds -= Duration.ofMinutes(controlRange).toSeconds();
            endTimestampSeconds -= Duration.ofMinutes(controlRange).toSeconds();
        }

        logger.info("No valid timestamp found within the specified range for metric: {}", metric);
        return null;
    }

    private MetricsTime getLastPeakTimestamp(PrometheusMetricResponseTimestamp response) {
        List<Result> results = response.getData().getResult();
        List<Double> maxNumericValues = new ArrayList<>();
        List<List<Double>> values = findMaxNumericValues(results, maxNumericValues);

        MetricsTime metricsTime = new MetricsTime();

        if (values != null && values.size() == maxNumericValues.size()) {
            metricsTime.time = "CurrentMetric";
            metricsTime.value = Instant.now();
        } else if (!maxNumericValues.isEmpty()) {
            Double maxPeakValueTimestamp = maxNumericValues.get(maxNumericValues.size() - 1);
            long epochSeconds = maxPeakValueTimestamp.longValue();
            metricsTime.time = "OneHourMetric";
            metricsTime.value = Instant.ofEpochSecond(epochSeconds);
        } else {
            metricsTime.time = "NullMetric";
            metricsTime.value = null;
        }
        return metricsTime;
    }

    private List<List<Double>> findMaxNumericValues(List<Result> results, List<Double> maxNumericValues) {
        List<List<Double>> values = null;

        for (Result result : results) {
            values = result.getValues();

            if (values != null && !values.isEmpty()) {
                for (int index = values.size() - 1; index >= 0; index--) {
                    Double numericValue = values.get(index).get(1);
                    if (numericValue != 0.0 && !isScientificNotation(numericValue)) {
                        maxNumericValues.add(values.get(index).get(0));
                    } else if (!maxNumericValues.isEmpty()) {
                        break;
                    }
                }
            }
        }

        logger.info("Created maxNumericValues");
        return values;
    }


    private boolean isScientificNotation(Double value) {
        String numericValue = value.toString();
        return numericValue.contains("e") || numericValue.contains("E");
    }


    public String findLastFoundTimestampAsString(Instant lastFoundInstant, ZoneId zoneId) {

        if (lastFoundInstant != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(zoneId);
            logger.info("Formatted timestamp to string in local time");
            return formatter.format(lastFoundInstant);
        }
        logger.info("No timestamp provided for formatting");
        return null;
    }


    private PrometheusMetricResponseTimestamp createUrlAndFetchData(String query, long startTimestamp, long endTimestamp, int step) {
        String apiUrl = "http://localhost:9090";
        logger.info("Creating URL");
        String encodedQuery = "sum(" + query + ")";
        String queryUrl = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .path("/api/v1/query_range")
                .queryParam("query", encodedQuery)
                .queryParam("start", startTimestamp)
                .queryParam("end", endTimestamp)
                .queryParam("step", step)
                .toUriString();
        return fetchDataFromApiUrl(queryUrl, PrometheusMetricResponseTimestamp.class);
    }

    private PrometheusMetricResponse fetchDataFromApi() {
        return fetchDataFromApiUrl(searchFromPrometheusApi, PrometheusMetricResponse.class);
    }

    private <T> T fetchDataFromApiUrl(String url, Class<T> responseType) {
        logger.info("Fetching Data from API url");
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    private String getColorForTimestamp(Instant timestamp) {
        logger.info("Getting Color");
        if (timestamp != null) {
            Instant now = Instant.now();
            Instant oneDayAgo = now.minus(Duration.ofDays(1));

            if (timestamp.isBefore(oneDayAgo)) {
                return redColor;
            } else if (timestamp.truncatedTo(ChronoUnit.MINUTES).equals(now.truncatedTo(ChronoUnit.MINUTES))) {
                return greenColor;
            } else if (timestamp.isAfter(oneDayAgo) && timestamp.isBefore(now)) {
                return yellowColor;
            }
        }
        logger.info("Color selected.");
        return whiteColor;
    }
}