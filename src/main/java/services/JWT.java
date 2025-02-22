package services;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import controllers.Config;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;

@ApplicationScoped
public class JWT {
    private SecretKey key;
    @Inject public JWT(Config config) {
        key = Keys.hmacShaKeyFor(
                config.getString("com.horstmann.codecheck.jwt.secret.key").getBytes());
    }
    public String generate(Map<String, Object> claims) {
        String jwt = Jwts.builder()
            .claims(claims)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(30))))
            .signWith(key)
            .compact();
        return jwt;
    }
    
    public Map<String, Object> verify(String token) {
        try {
            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token) // TODO: Was parseClaimsJws
                .getPayload();
        } catch (JwtException e) {
            throw new ServiceException("Invalid token " + token);
        }
    }
}
