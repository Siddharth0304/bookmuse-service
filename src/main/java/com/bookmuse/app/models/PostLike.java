package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="post_likes") @Getter @Setter
public class PostLike extends BaseEntity {
    private UUID userId;
    private UUID postId;
}
