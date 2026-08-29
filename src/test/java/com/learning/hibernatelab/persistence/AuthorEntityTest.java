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
class AuthorEntityTest {

    @Autowired
    private AuthorRepository authorRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsFields() {
        String name = "Joshua Bloch " + UUID.randomUUID();

        Author author = new Author();
        author.setName(name);
        Author saved = authorRepository.save(author);

        entityManager.flush();
        entityManager.clear();

        Author reloaded = authorRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getName()).isEqualTo(name);
        // A fresh author has no books.
        assertThat(reloaded.getBooks()).isEmpty();
    }

    @Test
    void allowsDuplicateNames() {
        // Author (Autor) is a person: names are intentionally NOT unique, so two
        // distinct authors may share the same name.
        String sharedName = "John Smith " + UUID.randomUUID();

        Author first = new Author();
        first.setName(sharedName);
        Author second = new Author();
        second.setName(sharedName);

        authorRepository.saveAll(List.of(first, second));
        entityManager.flush();
        entityManager.clear();

        List<Author> found = authorRepository.findAll().stream()
                .filter(a -> sharedName.equals(a.getName()))
                .toList();

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Author::getId).doesNotHaveDuplicates();
    }
}
