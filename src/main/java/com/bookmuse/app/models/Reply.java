package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="replies") @Getter @Setter
public class Reply extends BaseEntity {
    private UUID userId;
    private UUID discussionId;
    private String body;
}
