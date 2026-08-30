package com.learning.hibernatelab.domain;

/**
 * An editor (Herausgeber) reduced to what a client needs to display and link to
 * them.
 *
 * <p>As with {@link AuthorRef}, {@code editor.name} is not unique (see migration
 * {@code V5}), so the {@code id} is what distinguishes two same-named editors.
 */
public record EditorRef(
        Long id,
        String name
) {}
