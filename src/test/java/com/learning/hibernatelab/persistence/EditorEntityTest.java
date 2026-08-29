package com.learning.hibernatelab.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EditorEntityTest {

    @Autowired
    private EditorRepository editorRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsFields() {
        String name = "Lisa Friendly " + UUID.randomUUID();

        Editor editor = new Editor();
        editor.setName(name);
        Editor saved = editorRepository.save(editor);

        entityManager.flush();
        entityManager.clear();

        Editor reloaded = editorRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getName()).isEqualTo(name);
        // A fresh editor has no books.
        assertThat(reloaded.getBooks()).isEmpty();
    }

    @Test
    void allowsDuplicateNames() {
        // Editor (Herausgeber) is a person: names are intentionally NOT unique,
        // so two distinct editors may share the same name.
        String sharedName = "Max Mustermann " + UUID.randomUUID();

        Editor first = new Editor();
        first.setName(sharedName);
        Editor second = new Editor();
        second.setName(sharedName);

        editorRepository.saveAll(List.of(first, second));
        entityManager.flush();
        entityManager.clear();

        List<Editor> found = editorRepository.findAll().stream()
                .filter(e -> sharedName.equals(e.getName()))
                .toList();

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Editor::getId).doesNotHaveDuplicates();
    }
}
