package com.bookmuse.app.service;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.exception.ApiException;
import com.bookmuse.app.models.Club;
import com.bookmuse.app.models.Post;
import com.bookmuse.app.models.User;
import com.bookmuse.app.repository.DomainRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class AuthorizationTest {
    private final DomainRepository repo=mock(DomainRepository.class);
    private final AccessService access=new AccessService(repo);
    private final NotificationService notifications=new NotificationService(repo,access);
    private User authenticate() {
        User u=new User();u.setId(UUID.randomUUID());u.setRole("USER");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getId().toString(),null,java.util.List.of()));
        when(repo.find(User.class,u.getId())).thenReturn(Optional.of(u));return u;
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void shouldPreventUsersFromEditingOthersPosts() {
        authenticate();UUID id=UUID.randomUUID();Post p=new Post();p.setUserId(UUID.randomUUID());when(repo.find(Post.class,id)).thenReturn(Optional.of(p));
        var service=new PostService(repo,access,notifications);
        assertThrows(ApiException.class,()->service.update(id,new Requests.Body("changed")));
    }
    @Test void shouldDenyAdminOperationsToUsers() { authenticate();assertThrows(ApiException.class,access::admin); }
    @Test void shouldDenyPostingToClubWithoutMembership() {
        authenticate();UUID id=UUID.randomUUID();when(repo.find(Club.class,id)).thenReturn(Optional.of(new Club()));
        var service=new ClubService(repo,access,notifications);
        assertThrows(ApiException.class,()->service.discuss(id,new Requests.DiscussionInput("title","body")));
    }
    @Test void shouldNotNotifyUsersAboutTheirOwnActions() {
        UUID id=UUID.randomUUID();notifications.send(id,id,"LIKE",UUID.randomUUID());verify(repo,never()).save(any());
    }
}
