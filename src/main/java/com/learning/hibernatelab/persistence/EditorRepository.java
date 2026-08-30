package com.learning.hibernatelab.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EditorRepository extends JpaRepository<Editor, Long> {

    // EditorMapper walks editor.books, so it is fetched up front. One collection
    // means one join — no cartesian product to worry about here.
    @Override
    @EntityGraph(attributePaths = "books")
    List<Editor> findAll();

    @EntityGraph(attributePaths = "books")
    Optional<Editor> findWithBooksById(Long id);
}
