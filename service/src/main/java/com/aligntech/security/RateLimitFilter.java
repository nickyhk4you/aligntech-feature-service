package com.aligntech.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;

    public RateLimitFilter(LettuceConnectionFactory connectionFactory) {
        RedisClient redisClient = RedisClient.create(
                "redis://" + connectionFactory.getHostName() + ":" + connectionFactory.getPort()
        );
        StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientId = getClientId(request);
        String endpoint = request.getRequestURI();

        Bucket bucket = resolveBucket(clientId, endpoint);

        if (bucket.tryConsume(1)) {
            long remainingTokens = bucket.getAvailableTokens();
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remainingTokens));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client: {} on endpoint: {}", clientId, endpoint);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-RateLimit-Retry-After", "60");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    private Bucket resolveBucket(String clientId, String endpoint) {
        String key = "rate-limit:" + clientId + ":" + endpoint;
        
        Supplier<BucketConfiguration> configSupplier = () -> {
            Bandwidth limit;
            
            if (endpoint.startsWith("/api/v1/admin")) {
                // Admin endpoints: 100 requests per minute
                limit = Bandwidth.simple(100, Duration.ofMinutes(1));
            } else if (endpoint.startsWith("/api/v1/evaluate") || endpoint.startsWith("/api/v1/snapshot")) {
                // Evaluation endpoints: 1000 requests per minute
                limit = Bandwidth.simple(1000, Duration.ofMinutes(1));
            } else {
                // Other endpoints: 500 requests per minute
                limit = Bandwidth.simple(500, Duration.ofMinutes(1));
            }
            
            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };

        return proxyManager.builder().build(key, configSupplier);
    }

    private String getClientId(HttpServletRequest request) {
        // Try to get authenticated user
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        
        // Fallback to API key
        String apiKey = request.getHeader("X-App-Key");
        if (apiKey != null && !apiKey.isEmpty()) {
            return "apikey:" + apiKey;
        }
        
        // Fallback to IP address
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip;
    }
}
