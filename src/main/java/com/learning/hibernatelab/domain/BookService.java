package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.BookRepository;
import com.learning.hibernatelab.persistence.Publisher;
import com.learning.hibernatelab.persistence.PublisherRepository;
import com.learning.hibernatelab.persistence.Tag;
import com.learning.hibernatelab.persistence.TagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reads books and owns the two relations whose owning side is {@link Book}:
 * the publisher foreign key and {@code book_tag}.
 *
 * <p>Author and editor links are not managed here. {@code author_book} and
 * {@code editor_book} are owned by {@code Author} and {@code Editor}, so they
 * belong to those services — see {@link AuthorService#addBook}.
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final PublisherRepository publisherRepository;
    private final TagRepository tagRepository;
    private final BookMapper bookMapper;

    @Transactional(readOnly = true)
    public List<BookView> findAll() {
        return bookMapper.toViews(bookRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<BookView> findById(Long id) {
        return bookRepository.findWithPublisherById(id).map(bookMapper::toView);
    }

    @Transactional
    public BookView create(String title, String description) {
        Book book = new Book();
        book.setTitle(title);
        book.setDescription(description);
        return bookMapper.toView(bookRepository.save(book));
    }

    @Transactional
    public BookView update(Long id, String title, String description) {
        Book book = require(id);
        book.setTitle(title);
        book.setDescription(description);
        return bookMapper.toView(book);
    }

    /**
     * Sets or clears the book's publisher.
     *
     * <p>{@code book.publisher_id} is nullable, so a null {@code publisherId}
     * detaches the book from its publisher rather than being rejected.
     */
    @Transactional
    public BookView setPublisher(Long bookId, Long publisherId) {
        Book book = require(bookId);

        if (publisherId == null) {
            book.setPublisher(null);
        } else {
            Publisher publisher = publisherRepository.findById(publisherId)
                    .orElseThrow(() -> new EntityNotFoundException("Publisher " + publisherId));
            book.setPublisher(publisher);
        }

        return bookMapper.toView(book);
    }

    /**
     * Tags a book. {@code book_tag} is owned by {@link Book}, so the write goes
     * through the book's own set.
     */
    @Transactional
    public BookView addTag(Long bookId, Long tagId) {
        Book book = require(bookId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new EntityNotFoundException("Tag " + tagId));

        book.getTags().add(tag);
        // Inverse side, kept consistent for the remainder of this transaction.
        tag.getBooks().add(book);

        return bookMapper.toView(book);
    }

    @Transactional
    public BookView removeTag(Long bookId, Long tagId) {
        Book book = require(bookId);

        book.getTags().removeIf(tag -> {
            if (!tag.getId().equals(tagId)) {
                return false;
            }
            tag.getBooks().remove(book);
            return true;
        });

        return bookMapper.toView(book);
    }

    /**
     * Deletes a book. Its {@code author_book}, {@code editor_book} and
     * {@code book_tag} rows are removed by the {@code ON DELETE CASCADE} added
     * in migration {@code V4} — Book only owns {@code book_tag}, so the database
     * is what clears the other two.
     */
    @Transactional
    public void delete(Long id) {
        bookRepository.delete(require(id));
    }

    private Book require(Long id) {
        return bookRepository.findWithPublisherById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book " + id));
    }
}
