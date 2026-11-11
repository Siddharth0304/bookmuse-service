package com.bookmuse.app.config;
import com.bookmuse.app.models.User;
import com.bookmuse.app.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;
import java.util.Map;
@Component @RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final DomainRepository repo;
    private final PasswordEncoder passwords;
    @Value("${app.admin-email}") private String email;
    @Value("${app.admin-password}") private String password;
    @Override @Transactional public void run(ApplicationArguments args) {
        if(repo.count(User.class,"where e.role='ADMIN'",Map.of())>0) return;
        if(password.length()<8 || password.length()>20 || !email.contains("@")) throw new IllegalArgumentException("Valid ADMIN_EMAIL and 8–20 character ADMIN_PASSWORD required");
        User admin=new User(); admin.setEmail(email.strip().toLowerCase(Locale.ROOT)); admin.setName("Administrator");
        admin.setPasswordHash(passwords.encode(password)); admin.setRole("ADMIN"); admin.setFavouriteGenres(""); repo.save(admin);
    }
}
