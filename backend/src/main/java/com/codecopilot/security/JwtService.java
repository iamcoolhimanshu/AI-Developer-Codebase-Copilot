package com.codecopilot.security;

import com.codecopilot.config.AppProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.function.Function;

@Service
public class JwtService {

    private final AppProperties.Security security;
    private final MACSigner signer;
    private final MACVerifier verifier;

    public JwtService(AppProperties properties) {
        this.security = properties.getSecurity();
        var key = new SecretKeySpec(security.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        try {
            this.signer = new MACSigner(key);
            this.verifier = new MACVerifier(key);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise JWT signer", e);
        }
    }

    public String createAccessToken(Long userId, String username, Set<String> roles) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(security.getJwtExpirationMs())))
                .claim("username", username)
                .claim("roles", roles)
                .claim("tokenType", "access")
                .build();
        return sign(claims);
    }

    public String createRefreshToken(Long userId) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(security.getRefreshExpirationMs())))
                .claim("tokenType", "refresh")
                .build();
        return sign(claims);
    }

    public boolean isValidRefreshToken(String token) {
        return isValid(token, c -> "refresh".equals(c.getClaim("tokenType")));
    }

    public boolean isValidAccessToken(String token) {
        return isValid(token, c -> "access".equals(c.getClaim("tokenType")));
    }

    private boolean isValid(String token, Function<JWTClaimsSet, Boolean> typeCheck) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(verifier)) {
                return false;
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();
            if (exp == null || exp.before(new Date())) {
                return false;
            }
            return typeCheck.apply(claims);
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        try {
            return Long.valueOf(SignedJWT.parse(token).getJWTClaimsSet().getSubject());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    public String extractUsername(String token) {
        try {
            return SignedJWT.parse(token).getJWTClaimsSet().getStringClaim("username");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        try {
            Object roles = SignedJWT.parse(token).getJWTClaimsSet().getClaim("roles");
            if (roles instanceof java.util.List<?> list) {
                return new java.util.HashSet<>((java.util.List<String>) list);
            }
        } catch (Exception ignored) {
        }
        return Set.of();
    }

    private String sign(JWTClaimsSet claims) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(signer);
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }
}