package com.learning.hibernatelab.persistence;

import jakarta.persistence.*;

import java.util.Set;

@Entity
public class Tag {

    @Id
    @GeneratedValue
    private Long id;
    private String name;

    @ManyToMany
    private Set<Book> books;

}
