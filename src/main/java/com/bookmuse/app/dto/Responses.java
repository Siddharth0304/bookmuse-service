package com.bookmuse.app.dto;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;

public final class Responses {
    private Responses() {}
    public record Tokens(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
    public record Profile(UUID id, String name, String bio, String avatarUrl, Set<String> favouriteGenres,
        long followers, long following, long booksRead) {}
    public record Account(UUID id, String email, String name, String role, boolean blocked) {}
    public record Member(UUID id, String name, String avatarUrl) {}
    public record BookView(UUID id, String title, String author, Set<String> genres, String summary, String coverUrl) {}
    public record LibraryView(UUID id, UUID bookId, String status, Integer progress) {}
    public record PostView(UUID id, UUID userId, UUID bookId, String body, Instant createdAt, long likes, long shares, long comments) {}
    public record CommentView(UUID id, UUID userId, UUID postId, String body, Instant createdAt) {}
    public record ClubView(UUID id, UUID creatorId, String name, String description) {}
    public record DiscussionView(UUID id, UUID userId, UUID clubId, String title, String body, Instant createdAt) {}
    public record ReplyView(UUID id, UUID userId, UUID discussionId, String body, Instant createdAt) {}
    public record NotificationView(UUID id, UUID actorId, String type, UUID resourceId, boolean read, Instant createdAt) {}
    public record Count(long count) {}
    public record Id(UUID id) {}
    public record PageView<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
        public static <T> PageView<T> of(Page<T> page) {
            return new PageView<>(page.getContent(),page.getNumber(),page.getSize(),page.getTotalElements(),page.getTotalPages());
        }
    }
}
