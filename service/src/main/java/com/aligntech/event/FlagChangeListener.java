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

    public FlagChangeListener(CacheManager cacheManager, ObjectMapper objectMapper) {
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            FlagChangeEvent event = objectMapper.readValue(message.getBody(), FlagChangeEvent.class);

            // clear caches, sse push
            clearCaches();
            broadcast(event);

        } catch (Exception e) {
            log.error("failed processing flag-change msg", e);
        }
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
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
        for (SseEmitter e : emitters) {
            try {
                e.send(SseEmitter.event().name("flag-change")
                        .data(objectMapper.writeValueAsString(event)));
            } catch (IOException ex) {
                emitters.remove(e);
            }
        }
    }
}
