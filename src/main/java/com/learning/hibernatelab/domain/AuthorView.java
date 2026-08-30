package com.learning.hibernatelab.domain;

import java.util.List;

public record AuthorView (
        Long id,
        String name,
        List<String> bookTitles
){}
