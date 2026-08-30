package com.learning.hibernatelab.domain;

import java.util.List;

public record TagView(
        Long id,
        String name,
        List<BookRef> books
) {
    public TagView {
        if (books == null) {
            books = List.of();
        } else {
            books = List.copyOf(books);
        }
    }
}
