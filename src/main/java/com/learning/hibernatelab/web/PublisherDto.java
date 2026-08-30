package com.learning.hibernatelab.web;

import java.util.List;

/** The REST representation of a publisher (Verlag). */
public record PublisherDto(
        Long id,
        String name,
        List<BookRefDto> books
) {}
