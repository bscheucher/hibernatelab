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
class PublisherEntityTest {

    @Autowired
    private PublisherRepository publisherRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAndReloadsFields() {
        String name = "Addison-Wesley " + UUID.randomUUID();

        Publisher publisher = new Publisher();
        publisher.setName(name);
        Publisher saved = publisherRepository.save(publisher);

        entityManager.flush();
        entityManager.clear();

        Publisher reloaded = publisherRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getName()).isEqualTo(name);
        // A fresh publisher has no books.
        assertThat(reloaded.getBooks()).isEmpty();
    }

    @Test
    void allowsDuplicateNames() {
        // A publisher's display name is not a natural key: two distinct houses
        // may share a name, so duplicate names must be allowed.
        String sharedName = "Springer " + UUID.randomUUID();

        Publisher first = new Publisher();
        first.setName(sharedName);
        Publisher second = new Publisher();
        second.setName(sharedName);

        publisherRepository.saveAll(List.of(first, second));
        entityManager.flush();
        entityManager.clear();

        List<Publisher> found = publisherRepository.findAll().stream()
                .filter(p -> sharedName.equals(p.getName()))
                .toList();

        assertThat(found).hasSize(2);
        assertThat(found).extracting(Publisher::getId).doesNotHaveDuplicates();
    }
}
