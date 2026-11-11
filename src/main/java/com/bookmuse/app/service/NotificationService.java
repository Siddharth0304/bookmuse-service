package com.bookmuse.app.service;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.mapper.Views;
import com.bookmuse.app.models.Notification;
import com.bookmuse.app.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class NotificationService {
    private final DomainRepository repo;
    private final AccessService access;
    public void send(UUID recipient,UUID actor,String type,UUID resource) {
        if(recipient.equals(actor)) return;
        Notification n=new Notification(); n.setUserId(recipient); n.setActorId(actor); n.setType(type); n.setResourceId(resource); repo.save(n);
    }
    @Transactional(readOnly=true) public Responses.PageView<Responses.NotificationView> list(Pageable page) {
        return Responses.PageView.of(repo.page(Notification.class,"where e.userId=:id order by e.createdAt desc,e.id",Map.of("id",access.id()),page).map(Views::notification));
    }
    @Transactional(readOnly=true) public Responses.Count unread() {
        return new Responses.Count(repo.count(Notification.class,"where e.userId=:id and e.read=false",Map.of("id",access.id())));
    }
    public void read(UUID id) { Notification n=access.get(Notification.class,id); access.owner(n.getUserId()); n.setRead(true); }
    public void readAll() { repo.execute("update Notification e set e.read=true,e.updatedAt=:now where e.userId=:id and e.read=false",Map.of("id",access.id(),"now",java.time.Instant.now())); }
}
