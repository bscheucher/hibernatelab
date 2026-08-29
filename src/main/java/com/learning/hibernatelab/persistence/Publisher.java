package com.learning.hibernatelab.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A publisher's display name is not a natural key — distinct houses can
    // share a name — so it is not unique.
    private String name;

    @OneToMany(mappedBy = "publisher")
    private Set<Book> books = new HashSet<>();
}
