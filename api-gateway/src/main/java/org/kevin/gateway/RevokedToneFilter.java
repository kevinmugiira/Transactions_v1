package org.kevin.gateway;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;


/**
 * Runs right after the token has been verified. If the token's id (jti) is on the Redis denylist
 * that the auth service fills at logout, the request is stopped with 401.
 * Requests without a JWT (login, register) simply pass through.
 */

public class RevokedToneFilter implements WebFilter {

    private static final String DENY_PREFIX = "deny:";

    private final ReactiveStringRedisTemplate redis;

    public RevokedToneFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> Mono.justOrEmpty(context.getAuthentication()))
                .filter(authentication -> authentication instanceof JwtAuthenticationToken)
                .flatMap(authentication -> Mono.justOrEmpty(((JwtAuthenticationToken) authentication).getToken().getId()))
                .flatMap(jti -> redis.hasKey(DENY_PREFIX + jti))
                .defaultIfEmpty(false)
                .flatMap(revoked -> {
                    if (revoked) {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                })
        ;
    }
}
