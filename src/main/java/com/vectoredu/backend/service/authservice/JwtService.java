package com.vectoredu.backend.service.authservice;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpirationMillis;

    @Value("${security.jwt.refresh-token-expiration-time}")
    private long refreshTokenExpirationMillis;

    private Instant jwtExpiration;
    private Instant refreshTokenExpiration;

    @PostConstruct
    public void init() {
        this.jwtExpiration = Instant.now().plusMillis(jwtExpirationMillis);
        this.refreshTokenExpiration = Instant.now().plusMillis(refreshTokenExpirationMillis);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        // Получаем роль пользователя
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5)) // Убираем префикс "ROLE_"
                .findFirst()
                .orElse("USER"); // Если роль не найдена, устанавливаем дефолтную роль

        // Добавляем роль в extraClaims
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", role);

        return generateToken(extraClaims, userDetails.getUsername(), jwtExpirationMillis);
    }


    public String generateToken(Map<String, Object> extraClaims, String email, long expirationMillis) {
        Instant expiration = Instant.now().plusMillis(expirationMillis);
        return buildToken(extraClaims, email, expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Instant expiration = Instant.now().plusMillis(refreshTokenExpirationMillis);
        return buildToken(new HashMap<>(), userDetails.getUsername(), expiration);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            String email,
            Instant expiration
    ) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(Date.from(expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Instant getExpirationTime() {
        return jwtExpiration;
    }

    public Instant getRefreshExpirationTime() {
        return refreshTokenExpiration;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isRefreshTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isRefreshTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

