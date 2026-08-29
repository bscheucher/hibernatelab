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
class BookPublisherRelationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void assignPublisherToBook() {
        // Unique names so the test doesn't collide with data already in the
        // shared database (publisher.name is unique).
        String suffix = UUID.randomUUID().toString();
        String bookTitle = "Effective Java " + suffix;
        String publisherName = "Addison-Wesley " + suffix;

        Publisher publisher = new Publisher();
        publisher.setName(publisherName);
        Publisher savedPublisher = publisherRepository.save(publisher);

        Book book = new Book();
        book.setTitle(bookTitle);
        // Book is the owning side of the many-to-one, so the FK is set here.
        book.setPublisher(savedPublisher);
        book = bookRepository.save(book);

        // Flush the publisher_id insert and drop the first-level cache so the
        // reload below actually hits the database instead of returning cached refs.
        entityManager.flush();
        entityManager.clear();

        // Owning side: the book resolves back to its publisher.
        Book reloaded = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(reloaded.getPublisher()).isNotNull();
        assertThat(reloaded.getPublisher().getName()).isEqualTo(publisherName);

        // Inverse side: the publisher lists the book among its books.
        Publisher reloadedPublisher = publisherRepository.findById(savedPublisher.getId()).orElseThrow();
        assertThat(reloadedPublisher.getBooks())
                .extracting(Book::getTitle)
                .contains(bookTitle);
    }
}
