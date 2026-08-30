package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Author;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(uses = RefMapper.class)
public interface AuthorMapper {
    AuthorView toView(Author author);
    List<AuthorView> toViews(List<Author> authors);
}
