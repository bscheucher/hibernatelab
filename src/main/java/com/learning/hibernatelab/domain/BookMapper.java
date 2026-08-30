package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(uses = RefMapper.class)
public interface BookMapper {

    @Mapping(target = "tagNames", source = "tags")
    BookView toView(Book book);

    List<BookView> toViews(List<Book> books);

    default String tagName(Tag tag) {
        return tag.getName();
    }
}
