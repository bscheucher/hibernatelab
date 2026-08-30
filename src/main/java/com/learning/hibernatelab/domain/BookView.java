package com.learning.hibernatelab.domain;

import java.util.List;

/**
 * A book with its relations resolved to references.
 *
 * <p>{@code publisher} may be null — {@code book.publisher_id} is nullable.
 * {@code tagNames} stays a list of plain strings because {@code tag.name} is
 * unique (migration {@code V4}): a tag's name really is its identity.
 */
public record BookView(
        Long id,
        String title,
        String description,
        PublisherRef publisher,
        List<AuthorRef> authors,
        List<EditorRef> editors,
        List<String> tagNames
) {
    public BookView {
        if (authors == null) {
            authors = List.of();
        } else {
            authors = List.copyOf(authors);
        }

        if (editors == null) {
            editors = List.of();
        } else {
            editors = List.copyOf(editors);
        }

        if (tagNames == null) {
            tagNames = List.of();
        } else {
            tagNames = List.copyOf(tagNames);
        }
    }
}
