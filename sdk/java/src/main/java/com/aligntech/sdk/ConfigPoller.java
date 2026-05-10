package com.aligntech.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background poller that fetches flag configurations from the server.
 * Polls {@code GET /api/v1/snapshot} on a fixed interval.
 */
class ConfigPoller implements AutoCloseable {

    private final String baseUrl;
    private final String appKey;
    private final Duration pollInterval;
    private final ConfigStore store;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile String traceParent;

    ConfigPoller(String baseUrl, String appKey, Duration pollInterval,
                 ConfigStore store, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.appKey = appKey;
        this.pollInterval = pollInterval;
        this.store = store;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "align-sdk-poller");
            t.setDaemon(true);
            return t;
        });
    }

    void setTraceParent(String traceParent) {
        this.traceParent = traceParent;
    }

    /**
     * Synchronously fetch the initial snapshot. Blocks until data is loaded.
     */
    void fetchInitial() {
        fetchSnapshot();
    }

    /**
     * Start background polling.
     */
    void start() {
        if (!started.compareAndSet(false, true)) return;
        scheduler.scheduleWithFixedDelay(
                this::fetchSnapshot,
                pollInterval.toMillis(),
                pollInterval.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    private void fetchSnapshot() {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/snapshot"))
                    .header("X-App-Key", appKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET();

            if (traceParent != null && !traceParent.isEmpty()) {
                requestBuilder.header("traceparent", traceParent);
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) return;

            JsonNode root = objectMapper.readTree(response.body());
            String version = root.get("version").asText();

            Map<String, FlagConfig> newFlags = new ConcurrentHashMap<>();
            for (JsonNode node : root.get("flags")) {
                String flagKey = node.get("flagKey").asText();
                String flagType = node.get("flagType").asText();
                String status = node.get("status").asText();

                @SuppressWarnings("unchecked")
                Map<String, Object> rollout = node.has("rollout") && !node.get("rollout").isNull()
                        ? objectMapper.convertValue(node.get("rollout"), Map.class)
                        : Map.of();

                newFlags.put(flagKey, new FlagConfig(flagKey, flagType, status, rollout));
            }

            store.replaceAll(version, newFlags);
        } catch (Exception e) {
            System.err.println("Align SDK: failed fetching snapshot: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }
}
