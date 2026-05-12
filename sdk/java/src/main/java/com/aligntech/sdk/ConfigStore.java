package com.aligntech.sdk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


class ConfigStore {

    private volatile Map<String, FlagConfig> flags = new ConcurrentHashMap<>();
    private volatile String version;

    void replaceAll(String version, Map<String, FlagConfig> newFlags) {
        this.version = version;
        this.flags = new ConcurrentHashMap<>(newFlags);
    }

    FlagConfig get(String flagKey) {
        return flags.get(flagKey);
    }

    String version() {
        return version;
    }

    int size() {
        return flags.size();
    }

    boolean isEmpty() {
        return flags.isEmpty();
    }
}
