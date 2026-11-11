package com.bookmuse.app.service;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.exception.ApiException;
import com.bookmuse.app.models.User;
import com.bookmuse.app.models.RefreshToken;
import com.bookmuse.app.repository.DomainRepository;
import com.bookmuse.app.repository.UserRepository;
import com.bookmuse.app.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class AuthServiceTest {
    private final DomainRepository repo=mock(DomainRepository.class);
    private final UserRepository users=mock(UserRepository.class);
    private final PasswordEncoder passwords=mock(PasswordEncoder.class);
    private final TokenService tokens=new TokenService("test-only-secret-at-least-thirty-two-bytes",900);
    private final AuthService service=new AuthService(repo,users,passwords,tokens);
    @BeforeEach void setup() { ReflectionTestUtils.setField(service,"refreshDays",30L); }
    @Test void shouldAlwaysRegisterUsersWithHashedPasswords() {
        when(passwords.encode("Password123")).thenReturn("hash");
        when(repo.save(any(User.class))).thenAnswer(inv->{User u=inv.getArgument(0);u.setId(UUID.randomUUID());return u;});
        service.register(new Requests.Register("Reader@Example.com","Password123","Reader"));
        var captor=org.mockito.ArgumentCaptor.forClass(User.class);verify(repo).save(captor.capture());
        assertEquals("USER",captor.getValue().getRole());assertEquals("hash",captor.getValue().getPasswordHash());
        assertEquals("reader@example.com",captor.getValue().getEmail());
    }
    @Test void shouldRejectBlockedUsersBeforeIssuingTokens() {
        User user=new User();user.setId(UUID.randomUUID());user.setBlocked(true);
        when(users.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        assertThrows(ApiException.class,()->service.login(new Requests.Login("reader@example.com","Password123")));
        verify(repo,never()).save(any(RefreshToken.class));
    }
    @Test void shouldRejectExpiredRefreshTokens() {
        RefreshToken r=new RefreshToken();r.setExpiresAt(Instant.now().minusSeconds(1));
        when(repo.list(RefreshToken.class,"where e.tokenHash=:hash",Map.of("hash",tokens.hash("expired")),1)).thenReturn(List.of(r));
        when(repo.lockedToken(tokens.hash("expired"))).thenReturn(Optional.of(r));
        assertThrows(ApiException.class,()->service.refresh("expired"));verify(repo,never()).delete(r);
    }
    @Test void shouldRotateRefreshTokensAndRemoveConsumedToken() {
        UUID id=UUID.randomUUID();User u=new User();u.setId(id);
        RefreshToken r=new RefreshToken();r.setUserId(id);r.setExpiresAt(Instant.now().plusSeconds(100));
        when(repo.list(RefreshToken.class,"where e.tokenHash=:hash",Map.of("hash",tokens.hash("old")),1)).thenReturn(List.of(r));
        when(repo.lockedToken(tokens.hash("old"))).thenReturn(Optional.of(r));when(users.findById(id)).thenReturn(Optional.of(u));
        var result=service.refresh("old");verify(repo).delete(r);assertEquals(id,tokens.verify(result.accessToken()));
        verify(repo).save(any(RefreshToken.class));
    }
}
