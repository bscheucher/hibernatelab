package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Author;
import com.learning.hibernatelab.persistence.AuthorRepository;
import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.BookRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reads authors and owns the author-to-book link.
 *
 * <p>Every method that returns a view maps inside the transaction: {@code
 * Author.books} is lazy and {@link AuthorMapper} walks it, so mapping in the
 * controller instead would throw {@code LazyInitializationException}.
 */
@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final AuthorMapper authorMapper;

    @Transactional(readOnly = true)
    public List<AuthorView> findAll() {
        return authorMapper.toViews(authorRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<AuthorView> findById(Long id) {
        return authorRepository.findWithBooksById(id).map(authorMapper::toView);
    }

    @Transactional
    public AuthorView create(String name) {
        Author author = new Author();
        author.setName(name);
        return authorMapper.toView(authorRepository.save(author));
    }

    /**
     * Links an author to a book.
     *
     * <p>{@code author_book} is owned by {@link Author} — its {@code books} field
     * carries the {@code @JoinTable} — so the join row appears only because the
     * author's set changed. Adding to {@code book.getAuthors()} alone would be
     * ignored at flush, which is the classic silent no-op in a bidirectional
     * many-to-many.
     */
    @Transactional
    public AuthorView addBook(Long authorId, Long bookId) {
        Author author = require(authorId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book " + bookId));

        author.getBooks().add(book);
        // The inverse side is not maintained by Hibernate. Update it so the rest
        // of this transaction sees a consistent object graph.
        book.getAuthors().add(author);

        return authorMapper.toView(author);
    }

    @Transactional
    public AuthorView removeBook(Long authorId, Long bookId) {
        Author author = require(authorId);

        author.getBooks().removeIf(book -> {
            if (!book.getId().equals(bookId)) {
                return false;
            }
            book.getAuthors().remove(author);
            return true;
        });

        return authorMapper.toView(author);
    }

    /**
     * Deletes an author. The {@code author_book} rows go with it: Hibernate
     * clears the owned collection, and migration {@code V4} put
     * {@code ON DELETE CASCADE} on the join table as a backstop.
     */
    @Transactional
    public void delete(Long id) {
        authorRepository.delete(require(id));
    }

    private Author require(Long id) {
        return authorRepository.findWithBooksById(id)
                .orElseThrow(() -> new EntityNotFoundException("Author " + id));
    }
}
