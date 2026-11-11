package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="notifications") @Getter @Setter
public class Notification extends BaseEntity {
    private UUID userId;
    private UUID actorId;
    private String type;
    private UUID resourceId;
    private boolean read;
}
