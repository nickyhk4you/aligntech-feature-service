package com.aligntech.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class FlagChangeListener implements MessageListener {

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    public FlagChangeListener(CacheManager cacheManager, ObjectMapper objectMapper,
                             io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        
        io.micrometer.core.instrument.Gauge.builder("sse.connections.active", emitters, List::size)
                .description("Number of active SSE connections")
                .register(meterRegistry);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            meterRegistry.counter("redis.pubsub.messages.received").increment();
            
            FlagChangeEvent event = objectMapper.readValue(message.getBody(), FlagChangeEvent.class);

            // clear caches, sse push
            clearCaches();
            broadcast(event);
            
            meterRegistry.counter("redis.pubsub.messages.processed").increment();

        } catch (Exception e) {
            log.error("failed processing flag-change msg", e);
            meterRegistry.counter("redis.pubsub.messages.failed").increment();
        }
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        
        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            meterRegistry.counter("sse.connections.closed", "reason", "completed").increment();
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            meterRegistry.counter("sse.connections.closed", "reason", "timeout").increment();
        });
        emitter.onError(e -> {
            emitters.remove(emitter);
            meterRegistry.counter("sse.connections.closed", "reason", "error").increment();
        });
        
        meterRegistry.counter("sse.connections.opened").increment();
        
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    private void clearCaches() {
        var snapshot = cacheManager.getCache("flagSnapshot");
        if (snapshot != null) snapshot.clear();
        var evals = cacheManager.getCache("flagEvaluations");
        if (evals != null) evals.clear();
    }

    private void broadcast(FlagChangeEvent event) {
        int successCount = 0;
        int failCount = 0;
        
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name("flag-change")
                        .data(objectMapper.writeValueAsString(event)));
                successCount++;
            } catch (IOException ex) {
                emitters.remove(e);
                failCount++;
            }
        }
        
        meterRegistry.counter("sse.messages.sent.success").increment(successCount);
        meterRegistry.counter("sse.messages.sent.failed").increment(failCount);
    }
    
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
