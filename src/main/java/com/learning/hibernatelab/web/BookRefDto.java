package com.learning.hibernatelab.web;

/** A book reduced to what a client needs to display and link to it. */
public record BookRefDto(Long id, String title) {}
