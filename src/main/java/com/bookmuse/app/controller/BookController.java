package com.bookmuse.app.controller;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.service.AccessService;
import com.bookmuse.app.service.BookService;
import com.bookmuse.app.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.List;
@RestController @RequestMapping("/api") @RequiredArgsConstructor
public class BookController {
    private final BookService service;
    private final AccessService access;
    private final RecommendationService recommendations;
    @Operation(summary="Search the book catalogue by title/author and any selected exact genre",
        description="Repeat genre or supply comma-separated values. Genres match with OR; search combines with AND. Genre matching is case-insensitive, ignores blanks and duplicates, and matches complete labels.") @GetMapping("/books")
    public Responses.PageView<Responses.BookView> books(@RequestParam(name="search",required=false) String search,
        @Parameter(description="Genres to match, e.g. genre=fantasy&genre=mystery or genre=fantasy,mystery")
        @RequestParam(name="genre",required=false) List<String> genres,@ParameterObject Pageable page) { return service.list(search,genres,page); }
    @Operation(summary="Get a book") @GetMapping("/books/{id}")
    public Responses.BookView book(@PathVariable UUID id) { return service.get(id); }
    @Operation(summary="ADMIN: add a book") @PostMapping("/books") @ResponseStatus(HttpStatus.CREATED)
    public Responses.BookView create(@Valid @RequestBody Requests.BookInput r) { return service.create(r); }
    @Operation(summary="ADMIN: replace book details") @PutMapping("/books/{id}")
    public Responses.BookView update(@PathVariable UUID id,@Valid @RequestBody Requests.BookInput r) { return service.update(id,r); }
    @Operation(summary="ADMIN: delete a book and dependent content") @DeleteMapping("/books/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
    @Operation(summary="List my library") @GetMapping("/me/library")
    public Responses.PageView<Responses.LibraryView> library(@ParameterObject Pageable page) { return service.library(access.id(),page); }
    @Operation(summary="List another reader's library") @GetMapping("/users/{id}/library")
    public Responses.PageView<Responses.LibraryView> library(@PathVariable UUID id,@ParameterObject Pageable page) { return service.library(id,page); }
    @Operation(summary="Add a book to my library or update its reading status") @PutMapping("/me/library/{bookId}")
    public Responses.LibraryView putLibrary(@PathVariable UUID bookId,@Valid @RequestBody Requests.LibraryInput r) { return service.putLibrary(bookId,r); }
    @Operation(summary="Remove a book from my library") @DeleteMapping("/me/library/{bookId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLibrary(@PathVariable UUID bookId) { service.removeLibrary(bookId); }
    @Operation(summary="Get genre-affinity recommendations, with popularity fallback") @GetMapping("/me/recommendations")
    public Responses.PageView<Responses.BookView> recommendations(@ParameterObject Pageable page) { return recommendations.recommendations(page); }
}
