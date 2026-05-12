package com.aligntech.sdk;

import java.time.Duration;

/**
 * Configuration for the SDK client.
 */
public class AlignTechConfig {

    final String baseUrl;
    final String appKey;
    final Duration pollInterval;

    private AlignTechConfig(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.appKey = builder.appKey;
        this.pollInterval = builder.pollInterval;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String baseUrl = "http://localhost:8080";
        private String appKey;
        private Duration pollInterval = Duration.ofSeconds(30);

        public Builder baseUrl(String v) { this.baseUrl = v; return this; }

        public Builder appKey(String v) { this.appKey = v; return this; }

        public Builder pollInterval(Duration v) { this.pollInterval = v; return this; }

        public AlignTechConfig build() {
            if (appKey == null || appKey.isBlank()) throw new IllegalArgumentException("appKey is required");
            return new AlignTechConfig(this);
        }
    }
}
