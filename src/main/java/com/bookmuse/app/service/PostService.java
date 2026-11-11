package com.bookmuse.app.service;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.mapper.Views;
import com.bookmuse.app.models.Book;
import com.bookmuse.app.models.Comment;
import com.bookmuse.app.models.Post;
import com.bookmuse.app.models.PostLike;
import com.bookmuse.app.models.Share;
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
public class PostService {
    private final DomainRepository repo;
    private final AccessService access;
    private final NotificationService notifications;
    private Responses.PostView view(Post p,long likes,long shares,long comments) { return new Responses.PostView(p.getId(),p.getUserId(),p.getBookId(),p.getBody(),p.getCreatedAt(),likes,shares,comments); }
    private Responses.PageView<Responses.PostView> views(Page<Post> p) {
        List<UUID> ids=p.getContent().stream().map(Post::getId).toList();
        var likes=repo.counts(PostLike.class,"postId",ids,"");var shares=repo.counts(Share.class,"postId",ids,"");var comments=repo.counts(Comment.class,"postId",ids,"");
        return Responses.PageView.of(p.map(e->view(e,likes.getOrDefault(e.getId(),0L),shares.getOrDefault(e.getId(),0L),comments.getOrDefault(e.getId(),0L))));
    }
    @Transactional(readOnly=true) public Responses.PageView<Responses.PostView> list(UUID bookId,Pageable p) {
        return views(repo.page(Post.class,(bookId==null?"":"where e.bookId=:book ")+"order by e.createdAt desc,e.id",bookId==null?Map.of():Map.of("book",bookId),p));
    }
    @Transactional(readOnly=true) public Responses.PostView get(UUID id) {
        Post p=access.get(Post.class,id);return view(p,repo.count(PostLike.class,"where e.postId=:id",Map.of("id",id)),repo.count(Share.class,"where e.postId=:id",Map.of("id",id)),repo.count(Comment.class,"where e.postId=:id",Map.of("id",id)));
    }
    public Responses.PostView create(Requests.PostInput r) { access.get(Book.class,r.bookId());Post p=new Post();p.setBookId(r.bookId());p.setUserId(access.id());p.setBody(r.body());repo.save(p);return view(p,0,0,0); }
    public Responses.PostView update(UUID id,Requests.Body r) { Post p=access.get(Post.class,id);access.owner(p.getUserId());p.setBody(r.body());return get(id); }
    public void delete(UUID id) { Post p=access.get(Post.class,id);access.owner(p.getUserId());repo.delete(p); }
    public void like(UUID id) {
        UUID user=access.id();repo.lockUser(user);Post p=access.get(Post.class,id);
        if(repo.count(PostLike.class,"where e.userId=:u and e.postId=:p",Map.of("u",user,"p",id))>0)return;
        PostLike like=new PostLike();like.setUserId(user);like.setPostId(id);repo.save(like);notifications.send(p.getUserId(),user,"LIKE",id);
    }
    public void unlike(UUID id) { repo.list(PostLike.class,"where e.userId=:u and e.postId=:p",Map.of("u",access.id(),"p",id),1).forEach(repo::delete); }
    public Responses.Id share(UUID id) {
        Post p=access.get(Post.class,id);Share s=new Share();s.setPostId(id);s.setUserId(access.id());repo.save(s);notifications.send(p.getUserId(),access.id(),"SHARE",id);return new Responses.Id(s.getId());
    }
    @Transactional(readOnly=true) public Responses.PageView<Responses.CommentView> comments(UUID id,Pageable page) {
        access.get(Post.class,id);return Responses.PageView.of(repo.page(Comment.class,"where e.postId=:p order by e.createdAt,e.id",Map.of("p",id),page).map(Views::comment));
    }
    public Responses.CommentView comment(UUID id,Requests.Body r) {
        Post p=access.get(Post.class,id);Comment c=new Comment();c.setPostId(id);c.setUserId(access.id());c.setBody(r.body());repo.save(c);notifications.send(p.getUserId(),access.id(),"COMMENT",id);return Views.comment(c);
    }
    public Responses.CommentView updateComment(UUID id,Requests.Body r) { Comment c=access.get(Comment.class,id);access.owner(c.getUserId());c.setBody(r.body());return Views.comment(c); }
    public void deleteComment(UUID id) { Comment c=access.get(Comment.class,id);access.owner(c.getUserId());repo.delete(c); }
}
