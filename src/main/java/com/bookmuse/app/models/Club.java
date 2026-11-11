package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="clubs") @Getter @Setter
public class Club extends BaseEntity {
    private UUID creatorId;
    private String name;
    private String description;
}
