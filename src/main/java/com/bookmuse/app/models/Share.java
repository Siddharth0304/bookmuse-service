package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="shares") @Getter @Setter
public class Share extends BaseEntity {
    private UUID userId;
    private UUID postId;
}
