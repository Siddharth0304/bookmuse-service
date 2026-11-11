package com.bookmuse.app.controller;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.service.PostService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class PostController {
    private final PostService service;
    @Operation(summary="List community posts, optionally filtered by book") @GetMapping("/posts")
    public Responses.PageView<Responses.PostView> list(@RequestParam(required=false) UUID bookId,@ParameterObject Pageable p) { return service.list(bookId,p); }
    @Operation(summary="Get a post and engagement counts") @GetMapping("/posts/{id}")
    public Responses.PostView get(@PathVariable UUID id) { return service.get(id); }
    @Operation(summary="Create a post about a book") @PostMapping("/posts") @ResponseStatus(HttpStatus.CREATED)
    public Responses.PostView create(@Valid @RequestBody Requests.PostInput r) { return service.create(r); }
    @Operation(summary="OWNER: edit a post") @PutMapping("/posts/{id}")
    public Responses.PostView update(@PathVariable UUID id,@Valid @RequestBody Requests.Body r) { return service.update(id,r); }
    @Operation(summary="OWNER: delete a post") @DeleteMapping("/posts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
    @Operation(summary="Like a post; idempotent") @PutMapping("/posts/{id}/like") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@PathVariable UUID id) { service.like(id); }
    @Operation(summary="Unlike a post; idempotent") @DeleteMapping("/posts/{id}/like") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@PathVariable UUID id) { service.unlike(id); }
    @Operation(summary="Record a share; each request adds a share event") @PostMapping("/posts/{id}/shares") @ResponseStatus(HttpStatus.CREATED)
    public Responses.Id share(@PathVariable UUID id) { return service.share(id); }
    @Operation(summary="List comments") @GetMapping("/posts/{id}/comments")
    public Responses.PageView<Responses.CommentView> comments(@PathVariable UUID id,@ParameterObject Pageable p) { return service.comments(id,p); }
    @Operation(summary="Comment on a post") @PostMapping("/posts/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
    public Responses.CommentView comment(@PathVariable UUID id,@Valid @RequestBody Requests.Body r) { return service.comment(id,r); }
    @Operation(summary="OWNER: edit a comment") @PutMapping("/comments/{id}")
    public Responses.CommentView updateComment(@PathVariable UUID id,@Valid @RequestBody Requests.Body r) { return service.updateComment(id,r); }
    @Operation(summary="OWNER: delete a comment") @DeleteMapping("/comments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable UUID id) { service.deleteComment(id); }
}
