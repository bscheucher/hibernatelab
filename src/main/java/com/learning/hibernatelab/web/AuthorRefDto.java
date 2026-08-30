package com.learning.hibernatelab.web;

/** An author reduced to what a client needs to display and link to them. */
public record AuthorRefDto(Long id, String name) {}
