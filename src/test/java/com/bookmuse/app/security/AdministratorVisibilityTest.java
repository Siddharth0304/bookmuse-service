package com.bookmuse.app.security;

import com.bookmuse.app.models.Book;
import com.bookmuse.app.models.Club;
import com.bookmuse.app.models.Comment;
import com.bookmuse.app.models.Discussion;
import com.bookmuse.app.models.Follow;
import com.bookmuse.app.models.LibraryEntry;
import com.bookmuse.app.models.Membership;
import com.bookmuse.app.models.Notification;
import com.bookmuse.app.models.Post;
import com.bookmuse.app.models.PostLike;
import com.bookmuse.app.models.RefreshToken;
import com.bookmuse.app.models.Reply;
import com.bookmuse.app.models.Share;
import com.bookmuse.app.models.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministratorVisibilityTest {
    private static StandardServiceRegistry registry;
    private static SessionFactory sessions;

    static Stream<Class<?>> protectedTypes() {
        return Stream.of(User.class,Post.class,Comment.class,Club.class,Discussion.class,Reply.class,
            Follow.class,Membership.class,LibraryEntry.class,PostLike.class,Share.class,Notification.class);
    }

    @BeforeAll static void buildQueryValidator() {
        // Validate the real HQL against entity metadata without an H2 database or JDBC connection.
        registry=new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect","org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access",false)
            .applySetting("hibernate.hbm2ddl.auto","none")
            .applySetting("hibernate.connection.provider_class","org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
            .build();
        MetadataSources metadata=new MetadataSources(registry);
        protectedTypes().forEach(metadata::addAnnotatedClass);
        metadata.addAnnotatedClass(Book.class).addAnnotatedClass(RefreshToken.class);
        sessions=metadata.buildMetadata().buildSessionFactory();
    }

    @AfterAll static void closeQueryValidator() {
        if(sessions!=null) sessions.close();
        if(registry!=null) StandardServiceRegistryBuilder.destroy(registry);
    }

    @AfterEach void clearCaller() { SecurityContextHolder.clearContext(); }

    private void caller(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            UUID.randomUUID().toString(),null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));
    }

    @ParameterizedTest @MethodSource("protectedTypes")
    void shouldRestrictAllUserBearingQueriesForNonAdministrators(Class<?> type) {
        caller("USER");
        String predicate=AdministratorVisibility.predicate(type);
        assertFalse(predicate.isBlank());
        try(Session session=sessions.openSession()) {
            assertDoesNotThrow(()->session.createQuery("select e from "+type.getSimpleName()
                +" e where e.id=:id and ("+predicate+") order by e.id",type));
            assertDoesNotThrow(()->session.createQuery("select count(e) from "+type.getSimpleName()
                +" e where ("+predicate+")",Long.class));
        }
    }

    @ParameterizedTest @MethodSource("protectedTypes")
    void shouldKeepAdministratorVisibilityForAdministrators(Class<?> type) {
        caller("ADMIN");
        assertEquals("",AdministratorVisibility.predicate(type));
    }

    @Test void shouldApplyToOtherNonAdministratorRoles() {
        caller("MODERATOR");
        assertTrue(AdministratorVisibility.restrictsCurrentViewer());
        assertFalse(AdministratorVisibility.predicate(User.class).isBlank());
    }

    @Test void shouldKeepBookCatalogueVisible() {
        caller("USER");
        assertEquals("",AdministratorVisibility.predicate(Book.class));
    }

    @Test void shouldAllowInternalBootstrapAndAuthenticationReadsWithoutACaller() {
        SecurityContextHolder.clearContext();
        assertEquals("",AdministratorVisibility.predicate(User.class));
    }
}
