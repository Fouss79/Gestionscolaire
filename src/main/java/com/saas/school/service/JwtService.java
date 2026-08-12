package com.saas.school.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final String SECRET = "super-secret-key-super-secret-key-super-secret";

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // 🔐 GENERATE TOKEN
    public String generateToken(String email, String role, List<String> permissions) {

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("permissions", permissions) // ✅ AJOUT ICI
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    // 📥 EMAIL (unique source)
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // 📥 ROLE
    public String extractRole(String token) {
        return (String) getClaims(token).get("role");
    }

    // 📥 PERMISSIONS
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Object permissions = getClaims(token).get("permissions");
        return permissions != null ? (List<String>) permissions : List.of();
    }

    // 🔍 CLAIMS
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ✅ VALIDATION
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}