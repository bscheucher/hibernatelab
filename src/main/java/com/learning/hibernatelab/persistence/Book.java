package com.learning.hibernatelab.persistence;

import jakarta.persistence.*;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private Author author;
    private String description;

    @ManyToMany
    private Tag tags;


}
