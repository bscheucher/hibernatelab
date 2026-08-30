package com.learning.hibernatelab.domain;

import java.util.List;

public record BookView(
        Long id,
        String title,
        String description,
        String publisherName,
        List<String> authorNames,
        List<String> tagNames) {}