package org.kevin.gateway.config;


import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.security.Principal;

/**
 * A key resolver decides WHO a request counts against. The Redis rate limiter keeps one token
 * bucket per key (see application.yml for the bucket sizes).
 */

@Configuration
public class RateLimitConfig {

    /** By client address: used for the login routes, where nobody is authenticated yet. */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(clientIp(exchange));
    }


    /** By user id (the token's "sub"); falls back to the address if there is no authenticated user. */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .defaultIfEmpty(clientIp(exchange));
    }

    private static String clientIp(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        return address != null && address.getAddress() != null ? address.getAddress().getHostAddress() : "unknown";
    }
}
