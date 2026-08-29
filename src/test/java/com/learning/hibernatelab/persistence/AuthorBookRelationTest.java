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
class AuthorBookRelationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void addAuthorToBook() {
        // Unique names keep the test isolated from data already in the shared
        // database (author.name is not unique, but this keeps assertions exact).
        String suffix = UUID.randomUUID().toString();
        String bookTitle = "Effective Java " + suffix;
        String authorName = "Joshua Bloch " + suffix;

        Book book = new Book();
        book.setTitle(bookTitle);
        book = bookRepository.save(book);

        Author author = new Author();
        author.setName(authorName);
        // Author is the owning side of the many-to-many, so the link is set here.
        author.getBooks().add(book);
        Author savedAuthor = authorRepository.save(author);

        // Flush the insert into author_book and drop the first-level cache so the
        // reload below actually hits the database instead of returning cached refs.
        entityManager.flush();
        entityManager.clear();

        Book reloaded = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(reloaded.getAuthors())
                .extracting(Author::getName)
                .containsExactly(authorName);

        Author reloadedAuthor = authorRepository.findById(savedAuthor.getId()).orElseThrow();
        assertThat(reloadedAuthor.getBooks())
                .extracting(Book::getTitle)
                .containsExactly(bookTitle);
    }
}
