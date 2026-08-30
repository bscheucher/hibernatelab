package com.learning.hibernatelab.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the four resources that are created from nothing but a name:
 * author, editor, publisher and tag.
 *
 * <p>Shared on purpose — four identical single-field records would say nothing
 * extra. Split it the moment one of them grows a field of its own.
 *
 * <p>The limit mirrors the schema: every one of those name columns is
 * {@code varchar(255)}. Without it an over-long name reaches the database and
 * comes back as a 500 from a constraint violation, rather than a 400 naming the
 * field.
 */
public record NameRequest(

        @NotBlank
        @Size(max = 255)
        String name
) {}
