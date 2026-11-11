package com.bookmuse.app.service;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.exception.ApiException;
import com.bookmuse.app.mapper.Views;
import com.bookmuse.app.models.Follow;
import com.bookmuse.app.models.LibraryEntry;
import com.bookmuse.app.models.User;
import com.bookmuse.app.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class ProfileService {
    private final DomainRepository repo;
    private final AccessService access;
    private final NotificationService notifications;
    @Transactional(readOnly=true) public Responses.Account account() { return Views.account(access.me()); }
    @Transactional(readOnly=true) public Responses.Profile profile(UUID id) {
        User u=access.get(User.class,id);
        return view(u,repo.count(Follow.class,"where e.followingId=:id",Map.of("id",id)),repo.count(Follow.class,"where e.followerId=:id",Map.of("id",id)),repo.count(LibraryEntry.class,"where e.userId=:id and e.status='READ'",Map.of("id",id)));
    }
    private Responses.Profile view(User u,long followers,long following,long read) { return new Responses.Profile(u.getId(),u.getName(),u.getBio(),u.getAvatarUrl(),Views.genres(u.getFavouriteGenres()),followers,following,read); }
    private Responses.PageView<Responses.Profile> profiles(Page<User> page) {
        List<UUID> ids=page.getContent().stream().map(User::getId).toList();
        var followers=repo.counts(Follow.class,"followingId",ids,""); var following=repo.counts(Follow.class,"followerId",ids,"");
        var read=repo.counts(LibraryEntry.class,"userId",ids,"and e.status='READ'");
        return Responses.PageView.of(page.map(u->view(u,followers.getOrDefault(u.getId(),0L),following.getOrDefault(u.getId(),0L),read.getOrDefault(u.getId(),0L))));
    }
    public Responses.Profile update(Requests.ProfileInput r) { User u=access.me();u.setName(r.name().strip());u.setBio(r.bio());u.setAvatarUrl(r.avatarUrl());u.setFavouriteGenres(Views.genres(r.favouriteGenres()));return profile(u.getId()); }
    @Transactional(readOnly=true) public Responses.PageView<Responses.Profile> readers(Pageable p) { return profiles(repo.page(User.class,"where e.blocked=false order by e.name,e.id",Map.of(),p)); }
    @Transactional(readOnly=true) public Responses.PageView<Responses.Profile> followers(UUID id,boolean following,Pageable p) {
        access.get(User.class,id);
        String sub=following?"select f.followingId from Follow f where f.followerId=:id":"select f.followerId from Follow f where f.followingId=:id";
        return profiles(repo.page(User.class,"where e.id in ("+sub+") order by e.name,e.id",Map.of("id",id),p));
    }
    public void follow(UUID target) {
        UUID me=access.id(); if(me.equals(target)) throw ApiException.conflict("Cannot follow yourself"); repo.lockUser(me);
        User u=access.get(User.class,target); if(u.isBlocked()) throw ApiException.forbidden();
        if(repo.count(Follow.class,"where e.followerId=:me and e.followingId=:target",Map.of("me",me,"target",target))>0) return;
        Follow f=new Follow();f.setFollowerId(me);f.setFollowingId(target);repo.save(f);notifications.send(target,me,"NEW_FOLLOWER",me);
    }
    public void unfollow(UUID target) { repo.list(Follow.class,"where e.followerId=:me and e.followingId=:target",Map.of("me",access.id(),"target",target),1).forEach(repo::delete); }
    @Transactional(readOnly=true) public Responses.PageView<Responses.Account> users(Pageable p) { access.admin();return Responses.PageView.of(repo.page(User.class,"order by e.createdAt desc,e.id",Map.of(),p).map(Views::account)); }
    public Responses.Account status(UUID id,boolean blocked) {
        access.admin();repo.lockUser(id);User u=access.get(User.class,id); if("ADMIN".equals(u.getRole())) throw ApiException.forbidden();u.setBlocked(blocked);
        if(blocked) u.setTokenVersion(u.getTokenVersion()+1);
        if(blocked) repo.execute("delete from RefreshToken e where e.userId=:id",Map.of("id",id));return Views.account(u);
    }
}
