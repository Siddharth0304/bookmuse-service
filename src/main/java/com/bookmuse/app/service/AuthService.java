package com.bookmuse.app.service;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.exception.ApiException;
import com.bookmuse.app.models.RefreshToken;
import com.bookmuse.app.models.User;
import com.bookmuse.app.repository.DomainRepository;
import com.bookmuse.app.repository.UserRepository;
import com.bookmuse.app.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class AuthService {
    private final DomainRepository repo;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    @Value("${app.refresh-days}") private long refreshDays;
    @jakarta.annotation.PostConstruct void validateConfiguration() {
        if(refreshDays<1 || refreshDays>365) throw new IllegalArgumentException("Refresh token lifetime must be 1–365 days");
    }
    public Responses.Tokens register(Requests.Register input) {
        String email=input.email().strip().toLowerCase(Locale.ROOT);
        if(users.existsByEmail(email)) throw ApiException.conflict("Email already registered");
        User user=new User(); user.setEmail(email); user.setName(input.name().strip());
        user.setPasswordHash(passwords.encode(input.password())); user.setRole("USER"); user.setFavouriteGenres(""); repo.save(user);
        return issue(user);
    }
    public Responses.Tokens login(Requests.Login input) {
        User user=users.findByEmail(input.email().strip().toLowerCase(Locale.ROOT)).orElseThrow(ApiException::unauthorized);
        repo.lockUser(user.getId());
        if(user.isBlocked() || !passwords.matches(input.password(),user.getPasswordHash())) throw ApiException.unauthorized();
        return issue(user);
    }
    public Responses.Tokens refresh(String raw) {
        String hash=tokens.hash(raw);
        RefreshToken candidate=repo.list(RefreshToken.class,"where e.tokenHash=:hash",Map.of("hash",hash),1)
            .stream().findFirst().orElseThrow(ApiException::unauthorized);
        // Always lock the user before the token, matching moderation's lock order.
        repo.lockUser(candidate.getUserId());
        RefreshToken token=repo.lockedToken(hash).orElseThrow(ApiException::unauthorized);
        if(!token.getExpiresAt().isAfter(Instant.now())) throw ApiException.unauthorized();
        User user=active(token.getUserId()); repo.delete(token); return issue(user);
    }
    public void logout(String raw) { repo.lockedToken(tokens.hash(raw)).ifPresent(repo::delete); }
    @Transactional(readOnly=true)
    public User active(UUID id) {
        User user=users.findById(id).orElseThrow(ApiException::unauthorized);
        if(user.isBlocked()) throw ApiException.unauthorized(); return user;
    }
    private Responses.Tokens issue(User user) {
        String raw=tokens.randomRefresh(); RefreshToken token=new RefreshToken(); token.setUserId(user.getId());
        token.setTokenHash(tokens.hash(raw)); token.setExpiresAt(Instant.now().plus(refreshDays,ChronoUnit.DAYS)); repo.save(token);
        return new Responses.Tokens(tokens.issue(user),raw,"Bearer",tokens.accessSeconds());
    }
}
