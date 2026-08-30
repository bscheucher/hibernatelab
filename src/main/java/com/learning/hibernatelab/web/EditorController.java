package com.learning.hibernatelab.web;

import com.learning.hibernatelab.domain.EditorService;
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

/** HTTP entry point for editors (Herausgeber) — the mirror of {@link AuthorController}. */
@RestController
@RequestMapping("/api/editors")
@RequiredArgsConstructor
public class EditorController {

    private final EditorService editorService;
    private final DtoMapper dtoMapper;

    @GetMapping
    public List<EditorDto> findAll() {
        return dtoMapper.toEditorDtos(editorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EditorDto> findById(@PathVariable Long id) {
        return editorService.findById(id)
                .map(dtoMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<EditorDto> create(@RequestBody NameRequest request) {
        EditorDto created = dtoMapper.toDto(editorService.create(request.name()));
        return ResponseEntity.created(URI.create("/api/editors/" + created.id())).body(created);
    }

    // editor_book is owned by Editor, so the link endpoint lives here.
    @PutMapping("/{id}/books/{bookId}")
    public EditorDto addBook(@PathVariable Long id, @PathVariable Long bookId) {
        return dtoMapper.toDto(editorService.addBook(id, bookId));
    }

    @DeleteMapping("/{id}/books/{bookId}")
    public EditorDto removeBook(@PathVariable Long id, @PathVariable Long bookId) {
        return dtoMapper.toDto(editorService.removeBook(id, bookId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        editorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
