package com.bookmuse.app.controller;

import com.bookmuse.app.config.SecurityConfig;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.models.User;
import com.bookmuse.app.security.JwtFilter;
import com.bookmuse.app.security.TokenService;
import com.bookmuse.app.service.AccessService;
import com.bookmuse.app.service.AuthService;
import com.bookmuse.app.service.BookService;
import com.bookmuse.app.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value=BookController.class,properties="app.frontend-origin=http://localhost:5173")
@Import({SecurityConfig.class,JwtFilter.class})
class BookControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean BookService service;
    @MockitoBean AccessService access;
    @MockitoBean RecommendationService recommendations;
    @MockitoBean TokenService tokens;
    @MockitoBean AuthService auth;

    @BeforeEach
    void authenticateBearerToken() {
        User user=new User();
        user.setId(UUID.randomUUID());
        user.setRole("USER");
        when(tokens.verify("test-access")).thenReturn(user.getId());
        when(auth.active(user.getId())).thenReturn(user);
    }

    @Test
    void shouldBindRepeatedGenresWithSearchAndPagination() throws Exception {
        when(service.list(eq("wizard"),eq(List.of("fantasy","mystery")),any(Pageable.class)))
            .thenReturn(new Responses.PageView<>(List.of(),1,5,0,0));
        mvc.perform(get("/api/books").header("Authorization","Bearer test-access").param("genre","fantasy","mystery")
                .param("search","wizard").param("page","1").param("size","5"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.size").value(5));
        verify(service).list(eq("wizard"),eq(List.of("fantasy","mystery")),
            eq(org.springframework.data.domain.PageRequest.of(1,5)));
    }

    @Test
    void shouldBindCommaSeparatedGenres() throws Exception {
        when(service.list(isNull(),eq(List.of("fantasy","mystery")),any(Pageable.class)))
            .thenReturn(new Responses.PageView<>(List.of(),0,20,0,0));
        mvc.perform(get("/api/books").header("Authorization","Bearer test-access").param("genre","fantasy,mystery"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void shouldKeepSingleGenreAndUnfilteredRequestsWorking() throws Exception {
        when(service.list(isNull(),eq(List.of("fantasy")),any(Pageable.class)))
            .thenReturn(new Responses.PageView<>(List.of(),0,20,0,0));
        when(service.list(isNull(),isNull(),any(Pageable.class)))
            .thenReturn(new Responses.PageView<>(List.of(),0,20,0,0));
        mvc.perform(get("/api/books").header("Authorization","Bearer test-access").param("genre","fantasy")).andExpect(status().isOk());
        mvc.perform(get("/api/books").header("Authorization","Bearer test-access")).andExpect(status().isOk());
        verify(service).list(isNull(),eq(List.of("fantasy")),any(Pageable.class));
        verify(service).list(isNull(),isNull(),any(Pageable.class));
    }

    @Test
    void shouldStillRequireAuthentication() throws Exception {
        mvc.perform(get("/api/books").param("genre","fantasy","mystery"))
            .andExpect(status().isUnauthorized());
    }
}
