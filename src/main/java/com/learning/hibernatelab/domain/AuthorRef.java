package com.learning.hibernatelab.domain;

/**
 * An author reduced to what a client needs to display and link to them.
 *
 * <p>The {@code id} matters: {@code author.name} is deliberately not unique
 * (see migration {@code V5}), so two different authors may share a name.
 */
public record AuthorRef(
        Long id,
        String name
) {}
