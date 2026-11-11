package com.bookmuse.app.models;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;
@MappedSuperclass @Getter @Setter
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID) private UUID id;
    @Column(nullable=false, updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    @PrePersist void create() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void update() { updatedAt = Instant.now(); }
    @Override public boolean equals(Object other) {
        return this==other || (other!=null && getClass()==other.getClass() && id!=null && id.equals(((BaseEntity)other).getId()));
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
