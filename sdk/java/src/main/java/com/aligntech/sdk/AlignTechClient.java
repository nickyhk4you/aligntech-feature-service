package com.aligntech.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AlignTechClient implements AutoCloseable {

    private final ConfigStore store;
    private final Evaluator evaluator;
    private final ConfigPoller poller;

    private AlignTechClient(AlignTechConfig config) {
        this.store = new ConfigStore();
        this.evaluator = new Evaluator();
        ObjectMapper mapper = new ObjectMapper();
        this.poller = new ConfigPoller(config.baseUrl, config.appKey,
                config.pollInterval, store, mapper);

        poller.fetchInitial();
        poller.start();
    }

    /**
     * Returns true if the given flag evaluates to "enabled" for this user.
     * If the flag is unknown, returns false (safe default).
     */
    public boolean isEnabled(String flagKey, AlignTechUser user) {
        FlagConfig flag = store.get(flagKey);
        if (flag == null) return false;
        return evaluator.evaluate(flag, user).enabled();
    }

    /**
     * Returns the evaluation result for a flag, or null if the flag is unknown.
     */
    public Evaluator.EvaluationResult evaluate(String flagKey, AlignTechUser user) {
        FlagConfig flag = store.get(flagKey);
        if (flag == null) return null;
        return evaluator.evaluate(flag, user);
    }

    public boolean isReady() {
        return !store.isEmpty();
    }

    /**
     * Propagate a W3C traceparent header to the server on snapshot requests.
     * Call this with the current trace context from your application's tracer
     * so the server can correlate SDK calls with your request traces.
     *
     * @param traceParent W3C traceparent value (e.g. "00-{traceId}-{spanId}-01")
     */
    public void setTraceParent(String traceParent) {
        poller.setTraceParent(traceParent);
    }

    @Override
    public void close() {
        poller.close();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private AlignTechConfig config;

        public Builder config(AlignTechConfig c) { this.config = c; return this; }

        public AlignTechClient build() {
            if (config == null) throw new IllegalArgumentException("config is required");
            return new AlignTechClient(config);
        }
    }
}
