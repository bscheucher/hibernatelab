package com.learning.hibernatelab.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Override
    @EntityGraph(attributePaths = "books")
    List<Tag> findAll();

    @EntityGraph(attributePaths = "books")
    Optional<Tag> findWithBooksById(Long id);

    // Safe as a single result: tag.name is unique (migration V4).
    Optional<Tag> findByName(String name);
}
