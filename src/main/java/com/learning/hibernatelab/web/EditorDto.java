package com.learning.hibernatelab.web;

import java.util.List;

/** The REST representation of an editor (Herausgeber). */
public record EditorDto(
        Long id,
        String name,
        List<BookRefDto> books
) {}
