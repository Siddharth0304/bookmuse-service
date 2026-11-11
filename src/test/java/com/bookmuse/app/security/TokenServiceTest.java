package com.bookmuse.app.security;
import com.bookmuse.app.exception.ApiException;
import com.bookmuse.app.models.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
class TokenServiceTest {
    private final TokenService tokens=new TokenService("a-test-only-secret-with-at-least-32-bytes",900);
    @Test void shouldVerifySignedTokensAndRejectTampering() {
        User u=new User();u.setId(UUID.randomUUID());
        String token=tokens.issue(u);assertEquals(u.getId(),tokens.verify(token));
        String[] parts=token.split("\\.");
        assertThrows(ApiException.class,()->tokens.verify(parts[0]+"."+parts[1]+".invalid"));
    }
    @Test void shouldRejectOldTokensAfterBlockingAndReactivation() {
        User u=new User();u.setId(UUID.randomUUID());String token=tokens.issue(u);
        tokens.checkVersion(token,u);u.setTokenVersion(1);
        assertThrows(ApiException.class,()->tokens.checkVersion(token,u));
    }
    @Test void shouldGenerateDistinctOpaqueRefreshTokensAndStableHashes() {
        String first=tokens.randomRefresh();assertNotEquals(first,tokens.randomRefresh());
        assertEquals(64,tokens.hash(first).length());assertEquals(tokens.hash(first),tokens.hash(first));
        assertNotEquals(first,tokens.hash(first));
    }
    @Test void shouldRejectShortSecrets() { assertThrows(IllegalArgumentException.class,()->new TokenService("short",900)); }
}
