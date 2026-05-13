package com.aligntech.observability;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class CacheInterceptor {

    private final CacheMonitor cacheMonitor;

    public CacheInterceptor(CacheMonitor cacheMonitor) {
        this.cacheMonitor = cacheMonitor;
    }

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = getMethod(joinPoint);
        if (method == null) {
            return joinPoint.proceed();
        }

        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        if (cacheable == null) {
            return joinPoint.proceed();
        }

        String cacheName = getCacheName(cacheable);
        
        long startTime = System.nanoTime();
        Object result;
        boolean cacheHit = false;

        try {
            result = joinPoint.proceed();
            
            long duration = System.nanoTime() - startTime;
            
            if (duration < 1_000_000) {
                cacheHit = true;
                cacheMonitor.recordHit(cacheName);
                log.trace("Cache HIT for cache '{}' ({}µs)", cacheName, duration / 1000);
            } else {
                cacheMonitor.recordMiss(cacheName);
                log.trace("Cache MISS for cache '{}' ({}ms)", cacheName, duration / 1_000_000);
            }
            
            return result;
        } catch (Throwable e) {
            cacheMonitor.recordMiss(cacheName);
            throw e;
        }
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) {
        try {
            String methodName = joinPoint.getSignature().getName();
            Class<?> targetClass = joinPoint.getTarget().getClass();
            
            for (Method method : targetClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return method;
                }
            }
        } catch (Exception e) {
            log.warn("Could not resolve method for cache monitoring", e);
        }
        return null;
    }

    private String getCacheName(Cacheable cacheable) {
        String[] cacheNames = cacheable.value();
        if (cacheNames.length > 0) {
            return cacheNames[0];
        }
        
        String[] cacheNamesAlt = cacheable.cacheNames();
        if (cacheNamesAlt.length > 0) {
            return cacheNamesAlt[0];
        }
        
        return "unknown";
    }
}
