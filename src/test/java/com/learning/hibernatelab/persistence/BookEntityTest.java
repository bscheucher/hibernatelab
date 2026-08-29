package com.learning.hibernatelab.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BookEntityTest {

    @Autowired
    private BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsFields() {
        String title = "Effective Java " + UUID.randomUUID();

        Book book = new Book();
        book.setTitle(title);
        book.setDescription("Best practices for the Java platform");
        Book saved = bookRepository.save(book);

        entityManager.flush();
        entityManager.clear();

        Book reloaded = bookRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getTitle()).isEqualTo(title);
        assertThat(reloaded.getDescription()).isEqualTo("Best practices for the Java platform");
        // Optional relations default to empty / null on a fresh book.
        assertThat(reloaded.getPublisher()).isNull();
        assertThat(reloaded.getAuthors()).isEmpty();
        assertThat(reloaded.getEditors()).isEmpty();
        assertThat(reloaded.getTags()).isEmpty();
    }

    @Test
    void persistsWithoutOptionalFields() {
        // Only title is required; description and publisher are optional.
        Book book = new Book();
        book.setTitle("Untitled " + UUID.randomUUID());
        Book saved = bookRepository.save(book);

        entityManager.flush();
        entityManager.clear();

        Book reloaded = bookRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getPublisher()).isNull();
    }
}
