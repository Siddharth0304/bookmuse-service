package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="memberships") @Getter @Setter
public class Membership extends BaseEntity {
    private UUID userId;
    private UUID clubId;
}
