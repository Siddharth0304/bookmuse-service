package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="discussions") @Getter @Setter
public class Discussion extends BaseEntity {
    private UUID userId;
    private UUID clubId;
    private String title;
    private String body;
}
