package com.learning.hibernatelab.web;

import java.util.List;

/**
 * The REST representation of a book.
 *
 * <p>{@code publisher} is null for a book without one — {@code publisher_id} is
 * nullable, and migration {@code V5} made it {@code ON DELETE SET NULL}.
 * {@code tagNames} stays a list of plain strings because {@code tag.name} is
 * unique ({@code V4}).
 */
public record BookDto(
        Long id,
        String title,
        String description,
        PublisherRefDto publisher,
        List<AuthorRefDto> authors,
        List<EditorRefDto> editors,
        List<String> tagNames
) {}
