package com.bookmuse.app.models;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="users") @Getter @Setter
public class User extends BaseEntity {
    private int tokenVersion;
    private String email;
    private String passwordHash;
    private String name;
    private String bio;
    private String avatarUrl;
    private String favouriteGenres;
    private String role;
    private boolean blocked;
}
