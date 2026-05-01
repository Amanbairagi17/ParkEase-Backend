package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.entity.CustomUserDetails;
import com.parkease.auth_service.service.JWTService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JWTServiceImpl implements JWTService {

    @Value("${security.jwt.secret}")
    private String secretKey;

    private SecretKey getKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        try{
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }
        catch (Exception e){
            throw new AccessDeniedException("Invalid JWT token");
        }
    }

    private Date extractExpiration(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration();
    }

    private boolean isTokenExpired(String token) {
        try{
            Date expirationDate = extractExpiration(token);
            return expirationDate.before(new Date());
        }
        catch (Exception e) {
            throw new AccessDeniedException("Token expired!");
        }
    }

    @Override
    public String generateToken(CustomUserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getUserId())
                .claim("roles", userDetails.getAuthorities()
                        .stream()
                        .map(auth -> auth.getAuthority())
                        .toList())
                .expiration(new Date(System.currentTimeMillis() + 1000*60*60*24))
                .issuedAt(new Date(System.currentTimeMillis()))
                .signWith(getKey())
                .compact();

    }

    @Override
    public boolean validateToken(String token, CustomUserDetails userDetails) {
        String username = extractUserName(token);

        if (isTokenExpired(token)) {
            throw new AccessDeniedException("Token expired");
        }
        return username.equals(userDetails.getUsername());
    }

    @Override
    public String extractUserName(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();

    }

//    public static void main(String[] args) {
//        JWTServiceImpl jwtService = new JWTServiceImpl();
//        System.out.println(jwtService.secretKey);
//    }
}
