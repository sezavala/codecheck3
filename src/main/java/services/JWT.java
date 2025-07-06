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

    // TODO: Duration.ofMinutes(30) would be better, but doesn't work when baked into pages
    // (LTIAssignment.launch, contrast with LTIAssignmentController.launch which issues a cookie)
    // Maybe have the services that those pages call (saveWork, lti/saveWork, lti/sendScore, lti/saveComment)
    // refresh the token?
    public String generate(Map<String, Object> claims) {
        String jwt = Jwts.builder()
            .claims(claims)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(Duration.ofDays(7))))
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
