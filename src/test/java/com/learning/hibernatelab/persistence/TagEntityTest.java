package com.learning.hibernatelab.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TagEntityTest {

    @Autowired
    private TagRepository tagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsFields() {
        String name = "java-" + UUID.randomUUID();

        Tag tag = new Tag();
        tag.setName(name);
        Tag saved = tagRepository.save(tag);

        entityManager.flush();
        entityManager.clear();

        Tag reloaded = tagRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getName()).isEqualTo(name);
        // A fresh tag has no books.
        assertThat(reloaded.getBooks()).isEmpty();
    }

    @Test
    void rejectsDuplicateNames() {
        // Unlike people and publishers, a tag is a controlled label whose name
        // IS its identity, so duplicate names must be rejected by the unique
        // constraint (migration V4).
        String name = "concurrency-" + UUID.randomUUID();

        Tag first = new Tag();
        first.setName(name);
        tagRepository.saveAndFlush(first);

        Tag duplicate = new Tag();
        duplicate.setName(name);

        // saveAndFlush goes through the repository proxy, so the DB constraint
        // violation is translated to Spring's DataIntegrityViolationException.
        assertThatThrownBy(() -> tagRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
