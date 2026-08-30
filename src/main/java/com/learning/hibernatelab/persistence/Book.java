package com.learning.hibernatelab.persistence;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @ManyToOne
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    // The three collections below are batch-fetched rather than join-fetched.
    // BookMapper touches all three, and one entity graph covering them would
    // be a three-way cartesian product; @BatchSize turns the N+1 into a
    // bounded handful of IN queries instead.
    @BatchSize(size = 50)
    @ManyToMany(mappedBy = "books")
    private Set<Author> authors = new HashSet<>();

    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(
            name = "book_tag",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @BatchSize(size = 50)
    @ManyToMany(mappedBy = "books")
    private Set<Editor> editors = new HashSet<>();
}
