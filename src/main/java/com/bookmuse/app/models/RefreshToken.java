package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="refresh_tokens") @Getter @Setter
public class RefreshToken extends BaseEntity {
    private UUID userId;
    private String tokenHash;
    private Instant expiresAt;
}
