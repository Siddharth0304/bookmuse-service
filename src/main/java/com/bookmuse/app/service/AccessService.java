package com.bookmuse.app.service;
import com.bookmuse.app.exception.ApiException;
import com.bookmuse.app.models.User;
import com.bookmuse.app.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AccessService {
    private final DomainRepository repo;
    public UUID id() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) throw ApiException.unauthorized();
        return UUID.fromString(auth.getName());
    }
    public User me() {
        User u=repo.find(User.class,id()).orElseThrow(ApiException::unauthorized);
        if(u.isBlocked()) throw ApiException.unauthorized(); return u;
    }
    public void admin() { if(!"ADMIN".equals(me().getRole())) throw ApiException.forbidden(); }
    public void owner(UUID owner) { if(!me().getId().equals(owner)) throw ApiException.forbidden(); }
    public <T> T get(Class<T> type,UUID id) { return repo.find(type,id).orElseThrow(ApiException::missing); }
}
