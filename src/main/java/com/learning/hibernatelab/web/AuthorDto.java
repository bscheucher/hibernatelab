package com.learning.hibernatelab.web;

import java.util.List;

/**
 * The REST representation of an author.
 *
 * <p>Structurally a twin of {@code AuthorView}, and deliberately a separate
 * type: this one is the published contract. A rename in the domain view is a
 * mapper change here, not a breaking change for clients.
 *
 * <p>No compact constructor is needed — the view it is mapped from has already
 * replaced nulls with empty lists.
 */
public record AuthorDto(
        Long id,
        String name,
        List<BookRefDto> books
) {}
