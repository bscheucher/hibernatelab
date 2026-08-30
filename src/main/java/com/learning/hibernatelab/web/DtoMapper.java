package com.learning.hibernatelab.web;

import com.learning.hibernatelab.domain.AuthorRef;
import com.learning.hibernatelab.domain.AuthorView;
import com.learning.hibernatelab.domain.BookRef;
import com.learning.hibernatelab.domain.BookView;
import com.learning.hibernatelab.domain.EditorRef;
import com.learning.hibernatelab.domain.EditorView;
import com.learning.hibernatelab.domain.PublisherRef;
import com.learning.hibernatelab.domain.PublisherView;
import com.learning.hibernatelab.domain.TagView;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Converts domain views into the REST contract.
 *
 * <p>One interface covers the whole layer rather than one per aggregate. The
 * reason {@code RefMapper} exists in {@code domain} — several mappers needing
 * the same conversion — does not apply here: there is only one mapper, so the
 * ref conversions have exactly one home already.
 *
 * <p>The list methods need distinct names because erasure makes
 * {@code List<AuthorView>} and {@code List<BookView>} the same signature.
 */
@Mapper
public interface DtoMapper {

    AuthorDto toDto(AuthorView view);

    EditorDto toDto(EditorView view);

    PublisherDto toDto(PublisherView view);

    TagDto toDto(TagView view);

    BookDto toDto(BookView view);

    List<AuthorDto> toAuthorDtos(List<AuthorView> views);

    List<EditorDto> toEditorDtos(List<EditorView> views);

    List<PublisherDto> toPublisherDtos(List<PublisherView> views);

    List<TagDto> toTagDtos(List<TagView> views);

    List<BookDto> toBookDtos(List<BookView> views);

    BookRefDto toDto(BookRef ref);

    AuthorRefDto toDto(AuthorRef ref);

    EditorRefDto toDto(EditorRef ref);

    PublisherRefDto toDto(PublisherRef ref);
}
