package com.kevin.transactionPrac.Transactions.security;


import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public record Caller (String userId, boolean admin) {

    public static Caller from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return new Caller(jwt.getSubject(), roles != null && roles.contains("ADMIN"));
    }
}
