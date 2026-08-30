package com.learning.hibernatelab.domain;

import java.util.List;

public record EditorView(
        Long id,
        String name,
        List<BookRef> books
) {
    public EditorView {
        if (books == null) {
            books = List.of();
        } else {
            books = List.copyOf(books);
        }
    }
}
