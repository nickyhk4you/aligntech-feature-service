package com.aligntech.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class CacheMonitor {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, CacheStats> cacheStatsMap = new ConcurrentHashMap<>();

    public CacheMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordHit(String cacheName) {
        getOrCreateStats(cacheName).recordHit();
    }

    public void recordMiss(String cacheName) {
        getOrCreateStats(cacheName).recordMiss();
    }

    public void recordEviction(String cacheName) {
        getOrCreateStats(cacheName).recordEviction();
    }

    private CacheStats getOrCreateStats(String cacheName) {
        return cacheStatsMap.computeIfAbsent(cacheName, name -> new CacheStats(name, meterRegistry));
    }

    public CacheMetrics getMetrics(String cacheName) {
        CacheStats stats = cacheStatsMap.get(cacheName);
        if (stats == null) {
            return new CacheMetrics(cacheName, 0, 0, 0, 0.0);
        }
        return stats.getMetrics();
    }

    public record CacheMetrics(
            String cacheName,
            long hits,
            long misses,
            long evictions,
            double hitRate
    ) {}

    private static class CacheStats {
        private final Counter hitCounter;
        private final Counter missCounter;
        private final Counter evictionCounter;

        CacheStats(String cacheName, MeterRegistry meterRegistry) {
            this.hitCounter = Counter.builder("cache.requests")
                    .tags(Arrays.asList(Tag.of("cache", cacheName), Tag.of("result", "hit")))
                    .description("Number of cache hits")
                    .register(meterRegistry);

            this.missCounter = Counter.builder("cache.requests")
                    .tags(Arrays.asList(Tag.of("cache", cacheName), Tag.of("result", "miss")))
                    .description("Number of cache misses")
                    .register(meterRegistry);

            this.evictionCounter = Counter.builder("cache.evictions")
                    .tag("cache", cacheName)
                    .description("Number of cache evictions")
                    .register(meterRegistry);

            meterRegistry.gauge("cache.hit.rate",
                    Arrays.asList(Tag.of("cache", cacheName)),
                    this,
                    stats -> stats.calculateHitRate());
        }

        void recordHit() {
            hitCounter.increment();
        }

        void recordMiss() {
            missCounter.increment();
        }

        void recordEviction() {
            evictionCounter.increment();
        }

        double calculateHitRate() {
            long hits = (long) hitCounter.count();
            long misses = (long) missCounter.count();
            long total = hits + misses;
            return total == 0 ? 0.0 : (double) hits / total;
        }

        CacheMonitor.CacheMetrics getMetrics() {
            long hits = (long) hitCounter.count();
            long misses = (long) missCounter.count();
            long evictions = (long) evictionCounter.count();
            return new CacheMonitor.CacheMetrics(
                    "unknown",
                    hits,
                    misses,
                    evictions,
                    calculateHitRate()
            );
        }
    }
}
