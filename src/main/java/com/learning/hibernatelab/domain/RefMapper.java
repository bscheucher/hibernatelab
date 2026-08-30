package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Author;
import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.Editor;
import com.learning.hibernatelab.persistence.Publisher;
import org.mapstruct.Mapper;

/**
 * The single home for entity to reference-record conversions.
 *
 * <p>Every view mapper needs at least one of these. Declaring them here once and
 * pulling them in with {@code @Mapper(uses = RefMapper.class)} beats redeclaring
 * them per mapper — and beats letting MapStruct generate a private copy into
 * each implementation, which is what happens when nobody declares them at all.
 *
 * <p>Overloading {@code toRef} is fine: MapStruct resolves by parameter type.
 */
@Mapper
public interface RefMapper {

    BookRef toRef(Book book);

    AuthorRef toRef(Author author);

    EditorRef toRef(Editor editor);

    PublisherRef toRef(Publisher publisher);
}
