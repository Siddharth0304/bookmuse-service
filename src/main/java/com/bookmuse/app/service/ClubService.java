package com.bookmuse.app.service;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.exception.ApiException;
import com.bookmuse.app.mapper.Views;
import com.bookmuse.app.models.Club;
import com.bookmuse.app.models.Discussion;
import com.bookmuse.app.models.Membership;
import com.bookmuse.app.models.Reply;
import com.bookmuse.app.models.User;
import com.bookmuse.app.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class ClubService {
    private final DomainRepository repo;
    private final AccessService access;
    private final NotificationService notifications;
    @Transactional(readOnly=true) public Responses.PageView<Responses.ClubView> list(Pageable p) { return Responses.PageView.of(repo.page(Club.class,"order by e.createdAt desc,e.id",Map.of(),p).map(Views::club)); }
    @Transactional(readOnly=true) public Responses.ClubView get(UUID id) { return Views.club(access.get(Club.class,id)); }
    public Responses.ClubView create(Requests.ClubInput r) {
        Club c=new Club();c.setCreatorId(access.id());c.setName(r.name().strip());c.setDescription(r.description());repo.save(c);
        Membership m=new Membership();m.setClubId(c.getId());m.setUserId(access.id());repo.save(m);return Views.club(c);
    }
    public Responses.ClubView update(UUID id,Requests.ClubInput r) { Club c=access.get(Club.class,id);access.owner(c.getCreatorId());c.setName(r.name().strip());c.setDescription(r.description());return Views.club(c); }
    public void delete(UUID id) { Club c=access.get(Club.class,id);access.owner(c.getCreatorId());repo.delete(c); }
    public void join(UUID id) {
        UUID user=access.id();repo.lockUser(user);Club c=access.get(Club.class,id);
        if(repo.count(Membership.class,"where e.userId=:u and e.clubId=:c",Map.of("u",user,"c",id))>0)return;
        Membership m=new Membership();m.setUserId(user);m.setClubId(id);repo.save(m);notifications.send(c.getCreatorId(),user,"CLUB_JOIN",id);
    }
    public void leave(UUID id) {
        Club c=access.get(Club.class,id);if(c.getCreatorId().equals(access.id()))throw ApiException.conflict("Creator must remain a member; delete the club to close it");
        repo.list(Membership.class,"where e.userId=:u and e.clubId=:c",Map.of("u",access.id(),"c",id),1).forEach(repo::delete);
    }
    private void member(UUID id) {
        access.get(Club.class,id);
        if(repo.count(Membership.class,"where e.userId=:u and e.clubId=:c",Map.of("u",access.id(),"c",id))==0)throw ApiException.forbidden();
    }
    @Transactional(readOnly=true) public Responses.PageView<Responses.Member> members(UUID id,Pageable p) {
        access.get(Club.class,id);
        return Responses.PageView.of(repo.page(User.class,"where e.id in (select m.userId from Membership m where m.clubId=:c) order by e.name,e.id",Map.of("c",id),p)
            .map(u->new Responses.Member(u.getId(),u.getName(),u.getAvatarUrl())));
    }
    @Transactional(readOnly=true) public Responses.PageView<Responses.DiscussionView> discussions(UUID id,Pageable p) {
        member(id);return Responses.PageView.of(repo.page(Discussion.class,"where e.clubId=:c order by e.createdAt desc,e.id",Map.of("c",id),p).map(Views::discussion));
    }
    public Responses.DiscussionView discuss(UUID id,Requests.DiscussionInput r) {
        member(id);Discussion d=new Discussion();d.setClubId(id);d.setUserId(access.id());d.setTitle(r.title());d.setBody(r.body());repo.save(d);return Views.discussion(d);
    }
    @Transactional(readOnly=true) public Responses.PageView<Responses.ReplyView> replies(UUID discussion,Pageable p) {
        Discussion d=access.get(Discussion.class,discussion);member(d.getClubId());
        return Responses.PageView.of(repo.page(Reply.class,"where e.discussionId=:d order by e.createdAt,e.id",Map.of("d",discussion),p).map(Views::reply));
    }
    public Responses.ReplyView reply(UUID discussion,Requests.Body r) {
        Discussion d=access.get(Discussion.class,discussion);member(d.getClubId());Reply reply=new Reply();reply.setDiscussionId(discussion);reply.setUserId(access.id());reply.setBody(r.body());repo.save(reply);
        notifications.send(d.getUserId(),access.id(),"DISCUSSION_REPLY",discussion);return Views.reply(reply);
    }
}
