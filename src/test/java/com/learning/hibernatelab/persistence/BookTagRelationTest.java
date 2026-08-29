package com.learning.hibernatelab.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BookTagRelationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TagRepository tagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void addTagToBook() {
        // Unique names so the test doesn't collide with data already in the
        // shared database (tag.name is unique).
        String suffix = UUID.randomUUID().toString();
        String bookTitle = "Effective Java " + suffix;
        String tagName = "java-" + suffix;

        Tag tag = new Tag();
        tag.setName(tagName);
        Tag savedTag = tagRepository.save(tag);

        Book book = new Book();
        book.setTitle(bookTitle);
        // Book is the owning side of the many-to-many, so the link is set here.
        book.getTags().add(savedTag);
        book = bookRepository.save(book);

        // Flush the insert into book_tag and drop the first-level cache so the
        // reload below actually hits the database instead of returning cached refs.
        entityManager.flush();
        entityManager.clear();

        Book reloaded = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(reloaded.getTags())
                .extracting(Tag::getName)
                .containsExactly(tagName);

        Tag reloadedTag = tagRepository.findById(savedTag.getId()).orElseThrow();
        assertThat(reloadedTag.getBooks())
                .extracting(Book::getTitle)
                .containsExactly(bookTitle);
    }
}
