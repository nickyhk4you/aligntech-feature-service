package com.aligntech.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class FlagChangePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(FlagChangeEvent event) {
        try {
            redisTemplate.convertAndSend("flag-changes", event);
            log.debug("Published flag change event: flagKey={}, action={}", event.getFlagKey(), event.getAction());
        } catch (Exception e) {
            log.error("Failed to publish flag change event for flagKey: {}", event.getFlagKey(), e);
        }
    }
}
