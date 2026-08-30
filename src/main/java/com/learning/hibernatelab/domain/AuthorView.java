package com.learning.hibernatelab.domain;

import java.util.List;

public record AuthorView(
        Long id,
        String name,
        List<BookRef> books
) {
    public AuthorView {
        if (books == null) {
            books = List.of();
        } else {
            books = List.copyOf(books);
        }
    }
}
