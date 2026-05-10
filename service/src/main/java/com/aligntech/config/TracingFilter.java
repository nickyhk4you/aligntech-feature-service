package com.aligntech.config;

import brave.Span;
import brave.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ensures trace context is reflected in response headers so callers can
 * extract the trace ID without parsing W3C traceparent themselves.
 */
@Component
public class TracingFilter extends OncePerRequestFilter {

    private final Tracer tracer;

    public TracingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        Span span = tracer.currentSpan();
        if (span != null) {
            String traceId = span.context().traceIdString();
            String spanId = span.context().spanIdString();
            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("traceparent",
                    String.format("00-%s-%s-%s", traceId, spanId,
                            Boolean.TRUE.equals(span.context().sampled()) ? "01" : "00"));
        }
    }
}
