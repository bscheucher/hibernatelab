package com.learning.hibernatelab.domain;

/**
 * A book reduced to what a client needs to display and link to it.
 *
 * <p>Reference records deliberately hold only scalars. That is what keeps the
 * views acyclic: a {@code BookRef} inside an {@link AuthorView} cannot lead back
 * to the authors again.
 */
public record BookRef(
        Long id,
        String title
) {}
