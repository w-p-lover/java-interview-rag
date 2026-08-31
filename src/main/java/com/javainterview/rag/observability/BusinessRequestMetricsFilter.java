package com.javainterview.rag.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BusinessRequestMetricsFilter extends OncePerRequestFilter {

    private static final String REQUEST_METRIC = "java.interview.business.requests";
    private static final String LATENCY_METRIC = "java.interview.business.request.latency";

    private final MeterRegistry meterRegistry;

    public BusinessRequestMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        response.setHeader("X-Request-Id", requestId);
        MDC.put("requestId", requestId);
        Timer.Sample sample = Timer.start(meterRegistry);
        RequestCompletion completion = new RequestCompletion(request, response, sample);
        try {
            filterChain.doFilter(request, response);
            if (request.isAsyncStarted()) {
                request.getAsyncContext().addListener(completion);
            } else {
                completion.complete(null);
            }
        } catch (IOException | ServletException | RuntimeException exception) {
            completion.complete(exception);
            throw exception;
        } finally {
            if (!request.isAsyncStarted()) {
                MDC.remove("requestId");
            }
        }
    }

    private final class RequestCompletion implements AsyncListener {
        private final HttpServletRequest request;
        private final HttpServletResponse response;
        private final Timer.Sample sample;
        private final AtomicBoolean completed = new AtomicBoolean();

        private RequestCompletion(HttpServletRequest request, HttpServletResponse response, Timer.Sample sample) {
            this.request = request;
            this.response = response;
            this.sample = sample;
        }

        private void complete(Throwable failure) {
            if (!completed.compareAndSet(false, true)) {
                return;
            }
            int status = failure == null ? response.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR.value();
            String outcome = status >= 500 ? "SERVER_ERROR" : status >= 400 ? "CLIENT_ERROR" : "SUCCESS";
            String operation = operationName();
            Counter.builder(REQUEST_METRIC)
                    .tags(Tags.of("operation", operation, "outcome", outcome))
                    .description("业务请求数量")
                    .register(meterRegistry)
                    .increment();
            sample.stop(Timer.builder(LATENCY_METRIC)
                    .tags("operation", operation)
                    .description("业务请求耗时")
                    .register(meterRegistry));
            MDC.remove("requestId");
        }

        private String operationName() {
            Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            return pattern instanceof String value && !value.isBlank()
                    ? request.getMethod() + " " + value
                    : request.getMethod() + " /api/unmatched";
        }

        @Override
        public void onComplete(AsyncEvent event) { complete(null); }

        @Override
        public void onTimeout(AsyncEvent event) { complete(new IllegalStateException("异步请求超时")); }

        @Override
        public void onError(AsyncEvent event) { complete(event.getThrowable()); }

        @Override
        public void onStartAsync(AsyncEvent event) { }
    }
}
