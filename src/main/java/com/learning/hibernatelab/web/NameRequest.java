package com.learning.hibernatelab.web;

/**
 * Request body for the four resources that are created from nothing but a name:
 * author, editor, publisher and tag.
 *
 * <p>Shared on purpose — four identical single-field records would say nothing
 * extra. Split it the moment one of them grows a field of its own.
 */
public record NameRequest(String name) {}
