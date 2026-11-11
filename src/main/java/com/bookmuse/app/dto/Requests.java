package com.bookmuse.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import java.util.Set;
import java.util.UUID;

public final class Requests {
    private Requests() {}
    public record Register(@Email @NotBlank @Size(max=254) String email,
        @NotBlank @Size(min=8,max=20) String password, @NotBlank @Size(max=100) String name) {}
    public record Login(@NotBlank @Email String email, @NotBlank @Size(min=8,max=20) String password) {}
    public record Refresh(@NotBlank @Size(max=256) String refreshToken) {}
    public record BookInput(@NotBlank @Size(max=255) String title, @NotBlank @Size(max=255) String author,
        @NotEmpty @Size(max=10) Set<@NotBlank @Size(max=50) @Pattern(regexp="[\\p{L}\\p{N} -]+") String> genres,
        @Size(max=10000) String summary, @URL @Size(max=255) String coverUrl) {}
    public record LibraryInput(@NotNull @Pattern(regexp="WANT_TO_READ|READING|READ") String status,
        @Min(0) @Max(100) Integer progress) {}
    public record PostInput(@NotNull UUID bookId, @NotBlank @Size(max=10000) String body) {}
    public record Body(@NotBlank @Size(max=10000) String body) {}
    public record ProfileInput(@NotBlank @Size(max=100) String name, @Size(max=2000) String bio,
        @URL @Size(max=255) String avatarUrl,
        @NotNull @Size(max=10) Set<@NotBlank @Size(max=50) @Pattern(regexp="[\\p{L}\\p{N} -]+") String> favouriteGenres) {}
    public record ClubInput(@NotBlank @Size(max=100) String name, @Size(max=10000) String description) {}
    public record DiscussionInput(@NotBlank @Size(max=255) String title, @NotBlank @Size(max=10000) String body) {}
    public record UserStatus(@NotNull Boolean blocked) {}
}
