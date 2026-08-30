package com.learning.hibernatelab.web;

import com.learning.hibernatelab.domain.AuthorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * HTTP entry point for authors.
 *
 * <p>Note what this class does <em>not</em> do: it never touches a repository,
 * an entity or a transaction. The service returns fully-resolved views, mapped
 * inside its own transaction, so there is no lazy collection left to blow up
 * once the session has closed.
 */
@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;
    private final DtoMapper dtoMapper;

    @GetMapping
    public List<AuthorDto> findAll() {
        return dtoMapper.toAuthorDtos(authorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorDto> findById(@PathVariable Long id) {
        return authorService.findById(id)
                .map(dtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AuthorDto> create(@Valid @RequestBody NameRequest request) {
        AuthorDto created = dtoMapper.toDto(authorService.create(request.name()));
        return ResponseEntity.created(URI.create("/api/authors/" + created.id())).body(created);
    }

    /**
     * Links an author to a book. This lives under {@code /authors} rather than
     * {@code /books} because {@code author_book} is owned by Author — the
     * endpoint follows the write.
     */
    @PutMapping("/{id}/books/{bookId}")
    public AuthorDto addBook(@PathVariable Long id, @PathVariable Long bookId) {
        return dtoMapper.toDto(authorService.addBook(id, bookId));
    }

    @DeleteMapping("/{id}/books/{bookId}")
    public AuthorDto removeBook(@PathVariable Long id, @PathVariable Long bookId) {
        return dtoMapper.toDto(authorService.removeBook(id, bookId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
