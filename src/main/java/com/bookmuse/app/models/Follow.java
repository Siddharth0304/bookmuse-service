package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="follows") @Getter @Setter
public class Follow extends BaseEntity {
    private UUID followerId;
    private UUID followingId;
}
