package com.learning.hibernatelab.web;

import com.learning.hibernatelab.domain.PublisherService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * HTTP entry point for publishers (Verlage).
 *
 * <p>Assigning a publisher to a book is {@code PUT
 * /api/books/{id}/publisher/{publisherId}} — the foreign key sits on Book, so
 * that is where the write belongs.
 */
@RestController
@RequestMapping("/api/publishers")
@RequiredArgsConstructor
public class PublisherController {

    private final PublisherService publisherService;
    private final DtoMapper dtoMapper;

    @GetMapping
    public List<PublisherDto> findAll() {
        return dtoMapper.toPublisherDtos(publisherService.findAll());
    }

    /**
     * Looks publishers up by name. Returns a list, unlike the tag equivalent:
     * migration {@code V6} dropped the uniqueness of {@code publisher.name}.
     */
    @GetMapping(params = "name")
    public List<PublisherDto> findByName(@RequestParam String name) {
        return dtoMapper.toPublisherDtos(publisherService.findByName(name));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublisherDto> findById(@PathVariable Long id) {
        return publisherService.findById(id)
                .map(dtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PublisherDto> create(@Valid @RequestBody NameRequest request) {
        PublisherDto created = dtoMapper.toDto(publisherService.create(request.name()));
        return ResponseEntity.created(URI.create("/api/publishers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public PublisherDto rename(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return dtoMapper.toDto(publisherService.rename(id, request.name()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        publisherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
