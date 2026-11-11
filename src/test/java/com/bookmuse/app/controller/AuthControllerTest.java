package com.bookmuse.app.controller;
import com.bookmuse.app.config.SecurityConfig;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.security.JwtFilter;
import com.bookmuse.app.security.TokenService;
import com.bookmuse.app.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(value=AuthController.class,properties="app.frontend-origin=http://localhost:5173")
@Import({SecurityConfig.class,JwtFilter.class})
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService service;
    @MockitoBean TokenService tokens;
    @Test void shouldRejectPasswordsLongerThanTwentyCharacters() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"reader@example.com\",\"password\":\"123456789012345678901\",\"name\":\"Reader\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors").isArray());
    }
    @Test void shouldRegisterWithoutAnExistingAccessToken() throws Exception {
        when(service.register(any(Requests.Register.class))).thenReturn(new Responses.Tokens("access","refresh","Bearer",900));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"reader@example.com\",\"password\":\"Password123\",\"name\":\"Reader\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
    @Test void shouldAllowOnlyTheConfiguredFrontendOrigin() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin","http://localhost:5173")
            .header("Access-Control-Request-Method","POST")).andExpect(status().isOk());
        mvc.perform(options("/api/auth/login").header("Origin","https://untrusted.example")
            .header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    }
}
