package com.bookmuse.app.controller;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.service.AccessService;
import com.bookmuse.app.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;
    private final AccessService access;
    @Operation(summary="Get my public profile and counts") @GetMapping("/me")
    public Responses.Profile me() { return service.profile(access.id()); }
    @Operation(summary="Get my private account information") @GetMapping("/me/account")
    public Responses.Account account() { return service.account(); }
    @Operation(summary="Update my profile") @PutMapping("/me")
    public Responses.Profile update(@Valid @RequestBody Requests.ProfileInput r) { return service.update(r); }
    @Operation(summary="Browse readers") @GetMapping("/users")
    public Responses.PageView<Responses.Profile> readers(@ParameterObject Pageable p) { return service.readers(p); }
    @Operation(summary="View a reader's public profile") @GetMapping("/users/{id}")
    public Responses.Profile profile(@PathVariable UUID id) { return service.profile(id); }
    @Operation(summary="Follow a reader; idempotent") @PutMapping("/users/{id}/follow") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable UUID id) { service.follow(id); }
    @Operation(summary="Unfollow a reader; idempotent") @DeleteMapping("/users/{id}/follow") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable UUID id) { service.unfollow(id); }
    @Operation(summary="List a reader's followers") @GetMapping("/users/{id}/followers")
    public Responses.PageView<Responses.Profile> followers(@PathVariable UUID id,@ParameterObject Pageable p) { return service.followers(id,false,p); }
    @Operation(summary="List readers followed by a reader") @GetMapping("/users/{id}/following")
    public Responses.PageView<Responses.Profile> following(@PathVariable UUID id,@ParameterObject Pageable p) { return service.followers(id,true,p); }
    @Operation(summary="ADMIN: list private user accounts") @GetMapping("/admin/users")
    public Responses.PageView<Responses.Account> users(@ParameterObject Pageable p) { return service.users(p); }
    @Operation(summary="ADMIN: block or activate a USER") @PutMapping("/admin/users/{id}/status")
    public Responses.Account status(@PathVariable UUID id,@Valid @RequestBody Requests.UserStatus r) { return service.status(id,r.blocked()); }
}
