package com.parkease.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    @Value("${security.jwt.secret}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new AccessDeniedException("Invalid JWT token");
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return extractAllClaims(token).get("userId", Long.class);
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private void validateTokenNotExpired(String token) {
        if (extractExpiration(token).before(new Date())) {
            throw new JwtException("Token expired!");
        }
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            validateTokenNotExpired(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims validateToken(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid or expired JWT token");
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {

        Claims claims = extractAllClaims(token);

        Object rolesObj = claims.get("roles");

        if (rolesObj == null) {
            throw new AccessDeniedException("No roles found in token");
        }

        if (!(rolesObj instanceof List<?>)) {
            throw new AccessDeniedException("Invalid roles format in token");
        }

        return (List<String>) rolesObj;
    }
}
