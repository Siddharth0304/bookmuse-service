package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Entity @Table(name="library_entries") @Getter @Setter
public class LibraryEntry extends BaseEntity {
    private UUID userId;
    private UUID bookId;
    private String status;
    private Integer progress;
}
