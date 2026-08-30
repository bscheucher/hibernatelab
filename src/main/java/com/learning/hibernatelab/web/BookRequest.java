package com.learning.hibernatelab.web;

/**
 * Request body for creating or updating a book.
 *
 * <p>Relations are not settable here. A book's publisher, tags, authors and
 * editors each have their own endpoint, because each is a distinct write
 * against whichever side owns the relation.
 */
public record BookRequest(String title, String description) {}
