package com.aligntech.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCache;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Collections;

@Configuration
@Profile("!test")
@Slf4j
public class CacheMetricsConfig {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;

    public CacheMetricsConfig(CacheManager cacheManager, MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void bindCacheMetrics() {
        log.info("Registering cache metrics");

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                bindCacheToMetrics(cacheName, cache);
            }
        }
    }

    private void bindCacheToMetrics(String cacheName, Cache cache) {
        try {
            if (cache instanceof RedisCache redisCache) {
                bindRedisCacheMetrics(cacheName, redisCache);
            } else {
                log.warn("Cache {} is not a RedisCache, skipping metrics binding", cacheName);
            }
        } catch (Exception e) {
            log.error("Failed to bind cache metrics for {}", cacheName, e);
        }
    }

    private void bindRedisCacheMetrics(String cacheName, RedisCache redisCache) {
        log.info("Binding metrics for Redis cache: {}", cacheName);

        try {
            Object nativeCache = redisCache.getNativeCache();
            Method getStatsMethod = nativeCache.getClass().getMethod("getStatistics");
            Object stats = getStatsMethod.invoke(nativeCache);

            if (stats != null) {
                CacheMeterBinder.bind(meterRegistry, redisCache, cacheName, Collections.emptyList());
                log.info("Successfully bound CacheMeterBinder for cache: {}", cacheName);
            }
        } catch (NoSuchMethodException e) {
            log.debug("Redis cache statistics not available, using custom metrics collector");
            bindCustomMetrics(cacheName);
        } catch (Exception e) {
            log.warn("Could not bind standard cache metrics for {}, using custom collector: {}", 
                    cacheName, e.getMessage());
            bindCustomMetrics(cacheName);
        }
    }

    private void bindCustomMetrics(String cacheName) {
        meterRegistry.gauge("cache.size", 
                Collections.singletonList(Tag.of("cache", cacheName)), 
                0);
        
        log.info("Registered custom metrics for cache: {}", cacheName);
    }
}
