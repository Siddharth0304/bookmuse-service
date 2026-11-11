package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="books") @Getter @Setter
public class Book extends BaseEntity {
    private String title;
    private String author;
    private String genres;
    private String summary;
    private String coverUrl;
}
