package com.bookmuse.app.controller;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.service.ClubService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class ClubController {
    private final ClubService service;
    @Operation(summary="List book clubs") @GetMapping("/clubs")
    public Responses.PageView<Responses.ClubView> list(@ParameterObject Pageable p) { return service.list(p); }
    @Operation(summary="Get a club") @GetMapping("/clubs/{id}")
    public Responses.ClubView get(@PathVariable UUID id) { return service.get(id); }
    @Operation(summary="Create a club and join as creator") @PostMapping("/clubs") @ResponseStatus(HttpStatus.CREATED)
    public Responses.ClubView create(@Valid @RequestBody Requests.ClubInput r) { return service.create(r); }
    @Operation(summary="CREATOR: manage club details") @PutMapping("/clubs/{id}")
    public Responses.ClubView update(@PathVariable UUID id,@Valid @RequestBody Requests.ClubInput r) { return service.update(id,r); }
    @Operation(summary="CREATOR: delete a club and its discussions") @DeleteMapping("/clubs/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
    @Operation(summary="Join a club; idempotent") @PutMapping("/clubs/{id}/membership") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@PathVariable UUID id) { service.join(id); }
    @Operation(summary="Leave a club; creator cannot leave") @DeleteMapping("/clubs/{id}/membership") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable UUID id) { service.leave(id); }
    @Operation(summary="List club members") @GetMapping("/clubs/{id}/members")
    public Responses.PageView<Responses.Member> members(@PathVariable UUID id,@ParameterObject Pageable p) { return service.members(id,p); }
    @Operation(summary="MEMBER: list discussion threads") @GetMapping("/clubs/{id}/discussions")
    public Responses.PageView<Responses.DiscussionView> discussions(@PathVariable UUID id,@ParameterObject Pageable p) { return service.discussions(id,p); }
    @Operation(summary="MEMBER: create a discussion thread") @PostMapping("/clubs/{id}/discussions") @ResponseStatus(HttpStatus.CREATED)
    public Responses.DiscussionView discuss(@PathVariable UUID id,@Valid @RequestBody Requests.DiscussionInput r) { return service.discuss(id,r); }
    @Operation(summary="MEMBER: list discussion replies") @GetMapping("/discussions/{id}/replies")
    public Responses.PageView<Responses.ReplyView> replies(@PathVariable UUID id,@ParameterObject Pageable p) { return service.replies(id,p); }
    @Operation(summary="MEMBER: reply to a discussion") @PostMapping("/discussions/{id}/replies") @ResponseStatus(HttpStatus.CREATED)
    public Responses.ReplyView reply(@PathVariable UUID id,@Valid @RequestBody Requests.Body r) { return service.reply(id,r); }
}
