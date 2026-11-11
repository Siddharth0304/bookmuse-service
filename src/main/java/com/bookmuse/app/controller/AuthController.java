package com.bookmuse.app.controller;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor @SecurityRequirements
public class AuthController {
    private final AuthService service;
    @Operation(summary="Register a USER account") @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public Responses.Tokens register(@Valid @RequestBody Requests.Register r) { return service.register(r); }
    @Operation(summary="Log in with email and password") @PostMapping("/login")
    public Responses.Tokens login(@Valid @RequestBody Requests.Login r) { return service.login(r); }
    @Operation(summary="Rotate a single-use refresh token") @PostMapping("/refresh")
    public Responses.Tokens refresh(@Valid @RequestBody Requests.Refresh r) { return service.refresh(r.refreshToken()); }
    @Operation(summary="Revoke a refresh token; access JWT expires at its original expiry") @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody Requests.Refresh r) { service.logout(r.refreshToken()); }
}
