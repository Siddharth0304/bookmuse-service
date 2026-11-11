package com.bookmuse.app.security;
import com.bookmuse.app.models.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import com.bookmuse.app.exception.ApiException;
@Component
public class TokenService {
    private final byte[] secret;
    private final long accessSeconds;
    public TokenService(@Value("${app.jwt-secret}") String secret,@Value("${app.access-seconds}") long seconds) {
        this.secret=secret.getBytes(StandardCharsets.UTF_8); this.accessSeconds=seconds;
        if(this.secret.length<32 || seconds<60) throw new IllegalArgumentException("JWT secret must contain at least 32 bytes and TTL at least 60 seconds");
    }
    public long accessSeconds() { return accessSeconds; }
    public String issue(User user) {
        Instant now=Instant.now();
        var claims=new JWTClaimsSet.Builder().issuer("bookmuse").audience("bookmuse-api")
            .subject(user.getId().toString()).issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(accessSeconds)))
            .claim("version",user.getTokenVersion()).jwtID(UUID.randomUUID().toString()).build();
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),claims);
        try { jwt.sign(new MACSigner(secret)); return jwt.serialize(); }
        catch(JOSEException ex) { throw new IllegalStateException("Token signing failed",ex); }
    }
    public UUID verify(String token) {
        try {
            var jwt=SignedJWT.parse(token); var c=jwt.getJWTClaimsSet();
            if(!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))
                || !"bookmuse".equals(c.getIssuer()) || !c.getAudience().contains("bookmuse-api")
                || c.getExpirationTime()==null || !c.getExpirationTime().toInstant().isAfter(Instant.now())) throw ApiException.unauthorized();
            return UUID.fromString(c.getSubject());
        } catch(Exception ex) { throw ApiException.unauthorized(); }
    }
    public String randomRefresh() { byte[] bytes=new byte[48]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public void checkVersion(String token,User user) {
        try {
            Number version=(Number)SignedJWT.parse(token).getJWTClaimsSet().getClaim("version");
            if(version==null || version.intValue()!=user.getTokenVersion()) throw ApiException.unauthorized();
        } catch(Exception ex) { throw ApiException.unauthorized(); }
    }
    public String hash(String raw) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
