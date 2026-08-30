package com.learning.hibernatelab.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a book.
 *
 * <p>Relations are not settable here. A book's publisher, tags, authors and
 * editors each have their own endpoint, because each is a distinct write
 * against whichever side owns the relation.
 *
 * <p>Sizes mirror migration {@code V1}: {@code title varchar(255)} and
 * {@code description varchar(2000)}. Description stays optional — the column is
 * nullable — so it carries no {@code @NotBlank}.
 */
public record BookRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 2000)
        String description
) {}
