package com.learning.hibernatelab.web;

import com.learning.hibernatelab.domain.BookService;
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
 * HTTP entry point for books.
 *
 * <p>Only the relations Book owns are settable here: the publisher foreign key
 * and {@code book_tag}. Authors and editors are linked through
 * {@link AuthorController} and {@link EditorController}, because their join
 * tables are owned by those entities.
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final DtoMapper dtoMapper;

    @GetMapping
    public List<BookDto> findAll() {
        return dtoMapper.toBookDtos(bookService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> findById(@PathVariable Long id) {
        return bookService.findById(id)
                .map(dtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BookDto> create(@RequestBody BookRequest request) {
        BookDto created = dtoMapper.toDto(bookService.create(request.title(), request.description()));
        return ResponseEntity.created(URI.create("/api/books/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public BookDto update(@PathVariable Long id, @RequestBody BookRequest request) {
        return dtoMapper.toDto(bookService.update(id, request.title(), request.description()));
    }

    @PutMapping("/{id}/publisher/{publisherId}")
    public BookDto setPublisher(@PathVariable Long id, @PathVariable Long publisherId) {
        return dtoMapper.toDto(bookService.setPublisher(id, publisherId));
    }

    /** Clears the publisher; {@code publisher_id} is nullable, so this is a normal state. */
    @DeleteMapping("/{id}/publisher")
    public BookDto clearPublisher(@PathVariable Long id) {
        return dtoMapper.toDto(bookService.setPublisher(id, null));
    }

    @PutMapping("/{id}/tags/{tagId}")
    public BookDto addTag(@PathVariable Long id, @PathVariable Long tagId) {
        return dtoMapper.toDto(bookService.addTag(id, tagId));
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public BookDto removeTag(@PathVariable Long id, @PathVariable Long tagId) {
        return dtoMapper.toDto(bookService.removeTag(id, tagId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
