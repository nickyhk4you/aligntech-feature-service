package com.aligntech.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequiredArgsConstructor
public class CacheMetricsController {

    private final CacheMonitor cacheMonitor;
    private final MeterRegistry meterRegistry;

    @GetMapping("/cache")
    public Map<String, Object> getCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        List<String> cacheNames = Arrays.asList("flagSnapshot", "flagEvaluations");
        
        for (String cacheName : cacheNames) {
            CacheMonitor.CacheMetrics cacheMetrics = cacheMonitor.getMetrics(cacheName);
            
            Map<String, Object> cacheData = new HashMap<>();
            cacheData.put("hits", cacheMetrics.hits());
            cacheData.put("misses", cacheMetrics.misses());
            cacheData.put("evictions", cacheMetrics.evictions());
            cacheData.put("hitRate", String.format("%.2f%%", cacheMetrics.hitRate() * 100));
            cacheData.put("totalRequests", cacheMetrics.hits() + cacheMetrics.misses());
            
            metrics.put(cacheName, cacheData);
        }
        
        Map<String, Object> overall = new HashMap<>();
        long totalHits = cacheNames.stream()
                .map(cacheMonitor::getMetrics)
                .mapToLong(CacheMonitor.CacheMetrics::hits)
                .sum();
        long totalMisses = cacheNames.stream()
                .map(cacheMonitor::getMetrics)
                .mapToLong(CacheMonitor.CacheMetrics::misses)
                .sum();
        long totalRequests = totalHits + totalMisses;
        double overallHitRate = totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests;
        
        overall.put("totalHits", totalHits);
        overall.put("totalMisses", totalMisses);
        overall.put("totalRequests", totalRequests);
        overall.put("overallHitRate", String.format("%.2f%%", overallHitRate * 100));
        
        metrics.put("overall", overall);
        
        return metrics;
    }

    @GetMapping("/cache/summary")
    public CacheSummary getCacheSummary() {
        CacheMonitor.CacheMetrics snapshotMetrics = cacheMonitor.getMetrics("flagSnapshot");
        CacheMonitor.CacheMetrics evalMetrics = cacheMonitor.getMetrics("flagEvaluations");
        
        long totalHits = snapshotMetrics.hits() + evalMetrics.hits();
        long totalMisses = snapshotMetrics.misses() + evalMetrics.misses();
        long totalRequests = totalHits + totalMisses;
        double hitRate = totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests;
        
        return new CacheSummary(
                totalRequests,
                totalHits,
                totalMisses,
                hitRate,
                List.of(
                        new CacheDetail("flagSnapshot", 
                                snapshotMetrics.hits(), 
                                snapshotMetrics.misses(), 
                                snapshotMetrics.hitRate()),
                        new CacheDetail("flagEvaluations", 
                                evalMetrics.hits(), 
                                evalMetrics.misses(), 
                                evalMetrics.hitRate())
                )
        );
    }

    public record CacheSummary(
            long totalRequests,
            long totalHits,
            long totalMisses,
            double overallHitRate,
            List<CacheDetail> caches
    ) {}

    public record CacheDetail(
            String name,
            long hits,
            long misses,
            double hitRate
    ) {}
}
