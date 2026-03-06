package com.ruchulu.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * LoggingFilter — global filter that logs every request through the gateway.
 * Logs method, path, status, and response time for observability.
 */
@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long startTime = Instant.now().toEpochMilli();

        String method = request.getMethod().name();
        String path   = request.getURI().getPath();
        String ip     = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        log.info("→ Gateway IN  [{} {}] from {}", method, path, ip);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = Instant.now().toEpochMilli() - startTime;
            int  status   = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;

            if (status >= 400) {
                log.warn("← Gateway OUT [{} {}] status={} time={}ms", method, path, status, duration);
            } else {
                log.info("← Gateway OUT [{} {}] status={} time={}ms", method, path, status, duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
