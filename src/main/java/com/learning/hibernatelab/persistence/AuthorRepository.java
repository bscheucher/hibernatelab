package com.learning.hibernatelab.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    // AuthorMapper walks author.books, so it is fetched up front. One collection
    // means one join — no cartesian product to worry about here.
    @Override
    @EntityGraph(attributePaths = "books")
    List<Author> findAll();

    @EntityGraph(attributePaths = "books")
    Optional<Author> findWithBooksById(Long id);
}
