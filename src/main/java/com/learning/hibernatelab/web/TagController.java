package com.learning.hibernatelab.web;

import com.learning.hibernatelab.domain.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * HTTP entry point for tags.
 *
 * <p>There is no endpoint for attaching a tag to a book: {@code book_tag} is
 * owned by Book, so that is {@code PUT /api/books/{id}/tags/{tagId}}.
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;
    private final DtoMapper dtoMapper;

    @GetMapping
    public List<TagDto> findAll() {
        return dtoMapper.toTagDtos(tagService.findAll());
    }

    /**
     * Looks a tag up by name. Returns a single object, not a list, because
     * {@code tag.name} is unique (migration {@code V4}).
     */
    @GetMapping(params = "name")
    public ResponseEntity<TagDto> findByName(@RequestParam String name) {
        return tagService.findByName(name)
                .map(dtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagDto> findById(@PathVariable Long id) {
        return tagService.findById(id)
                .map(dtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Creates the tag, or returns the existing one with this name. Idempotent by
     * design — the unique constraint on {@code tag.name} makes a plain insert a
     * coin flip against concurrent callers.
     */
    @PostMapping
    public ResponseEntity<TagDto> create(@Valid @RequestBody NameRequest request) {
        TagDto tag = dtoMapper.toDto(tagService.findOrCreate(request.name()));
        return ResponseEntity.created(URI.create("/api/tags/" + tag.id())).body(tag);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
