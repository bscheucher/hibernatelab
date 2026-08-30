package com.learning.hibernatelab.domain;

import java.util.List;

public record PublisherView(
        Long id,
        String name,
        List<String> bookTitles
) {}
