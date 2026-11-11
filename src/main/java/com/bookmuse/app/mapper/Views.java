package com.bookmuse.app.mapper;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.models.Book;
import com.bookmuse.app.models.Club;
import com.bookmuse.app.models.Comment;
import com.bookmuse.app.models.Discussion;
import com.bookmuse.app.models.LibraryEntry;
import com.bookmuse.app.models.Notification;
import com.bookmuse.app.models.Reply;
import com.bookmuse.app.models.User;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
public final class Views {
    private Views() {}
    public static String genres(Set<String> genres) { return genres.stream().map(s->s.strip().toLowerCase(Locale.ROOT)).distinct().sorted().collect(Collectors.joining("|")); }
    public static Set<String> genres(String genres) {
        return genres==null || genres.isBlank() ? Set.of() : new TreeSet<>(Arrays.asList(genres.split("\\|")));
    }
    public static Responses.BookView book(Book b) { return new Responses.BookView(b.getId(),b.getTitle(),b.getAuthor(),genres(b.getGenres()),b.getSummary(),b.getCoverUrl()); }
    public static Responses.LibraryView library(LibraryEntry e) { return new Responses.LibraryView(e.getId(),e.getBookId(),e.getStatus(),e.getProgress()); }
    public static Responses.Account account(User u) { return new Responses.Account(u.getId(),u.getEmail(),u.getName(),u.getRole(),u.isBlocked()); }
    public static Responses.ClubView club(Club c) { return new Responses.ClubView(c.getId(),c.getCreatorId(),c.getName(),c.getDescription()); }
    public static Responses.CommentView comment(Comment c) { return new Responses.CommentView(c.getId(),c.getUserId(),c.getPostId(),c.getBody(),c.getCreatedAt()); }
    public static Responses.DiscussionView discussion(Discussion d) { return new Responses.DiscussionView(d.getId(),d.getUserId(),d.getClubId(),d.getTitle(),d.getBody(),d.getCreatedAt()); }
    public static Responses.ReplyView reply(Reply r) { return new Responses.ReplyView(r.getId(),r.getUserId(),r.getDiscussionId(),r.getBody(),r.getCreatedAt()); }
    public static Responses.NotificationView notification(Notification n) { return new Responses.NotificationView(n.getId(),n.getActorId(),n.getType(),n.getResourceId(),n.isRead(),n.getCreatedAt()); }
}
