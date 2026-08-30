package com.learning.hibernatelab.domain;

/**
 * A publisher (Verlag) reduced to what a client needs to display and link to it.
 *
 * <p>{@code publisher.name} is not unique either (see migration {@code V6}: a
 * display name is not a natural key), so the {@code id} carries the identity.
 */
public record PublisherRef(
        Long id,
        String name
) {}
