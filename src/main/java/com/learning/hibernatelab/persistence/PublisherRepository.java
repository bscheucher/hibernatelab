package com.learning.hibernatelab.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    // PublisherMapper walks publisher.books, so it is fetched up front. One
    // collection means one join — no cartesian product to worry about here.
    @Override
    @EntityGraph(attributePaths = "books")
    List<Publisher> findAll();

    @EntityGraph(attributePaths = "books")
    Optional<Publisher> findWithBooksById(Long id);

    // Returns a list, not an Optional: migration V6 dropped the uniqueness on
    // publisher.name, so two houses may legitimately share one.
    @EntityGraph(attributePaths = "books")
    List<Publisher> findByName(String name);
}
