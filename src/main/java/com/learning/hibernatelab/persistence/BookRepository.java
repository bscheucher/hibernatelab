package com.learning.hibernatelab.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    // Only the publisher is join-fetched. @ManyToOne is EAGER by default, so
    // without this a list of books costs one extra query per book. The three
    // collections are left to @BatchSize on the entity — joining them here
    // would multiply the result set.
    @Override
    @EntityGraph(attributePaths = "publisher")
    List<Book> findAll();

    @EntityGraph(attributePaths = "publisher")
    Optional<Book> findWithPublisherById(Long id);
}
