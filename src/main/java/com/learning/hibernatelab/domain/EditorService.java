package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Book;
import com.learning.hibernatelab.persistence.BookRepository;
import com.learning.hibernatelab.persistence.Editor;
import com.learning.hibernatelab.persistence.EditorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reads editors (Herausgeber) and owns the editor-to-book link.
 *
 * <p>The mirror image of {@link AuthorService}: {@code editor_book} is owned by
 * {@link Editor}, so this is where that relation is written. Everything that
 * returns a view maps inside the transaction, because {@code Editor.books} is
 * lazy and {@link EditorMapper} walks it.
 */
@Service
@RequiredArgsConstructor
public class EditorService {

    private final EditorRepository editorRepository;
    private final BookRepository bookRepository;
    private final EditorMapper editorMapper;

    @Transactional(readOnly = true)
    public List<EditorView> findAll() {
        return editorMapper.toViews(editorRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<EditorView> findById(Long id) {
        return editorRepository.findWithBooksById(id).map(editorMapper::toView);
    }

    @Transactional
    public EditorView create(String name) {
        Editor editor = new Editor();
        editor.setName(name);
        return editorMapper.toView(editorRepository.save(editor));
    }

    /**
     * Links an editor to a book.
     *
     * <p>{@code editor_book} is owned by {@link Editor} — its {@code books} field
     * carries the {@code @JoinTable} — so the join row appears only because the
     * editor's set changed. Adding to {@code book.getEditors()} alone would be
     * ignored at flush.
     */
    @Transactional
    public EditorView addBook(Long editorId, Long bookId) {
        Editor editor = require(editorId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book " + bookId));

        editor.getBooks().add(book);
        // The inverse side is not maintained by Hibernate. Update it so the rest
        // of this transaction sees a consistent object graph.
        book.getEditors().add(editor);

        return editorMapper.toView(editor);
    }

    @Transactional
    public EditorView removeBook(Long editorId, Long bookId) {
        Editor editor = require(editorId);

        editor.getBooks().removeIf(book -> {
            if (!book.getId().equals(bookId)) {
                return false;
            }
            book.getEditors().remove(editor);
            return true;
        });

        return editorMapper.toView(editor);
    }

    /**
     * Deletes an editor. The {@code editor_book} rows go with it: Hibernate
     * clears the owned collection, and migration {@code V4} put
     * {@code ON DELETE CASCADE} on the join table as a backstop.
     */
    @Transactional
    public void delete(Long id) {
        editorRepository.delete(require(id));
    }

    private Editor require(Long id) {
        return editorRepository.findWithBooksById(id)
                .orElseThrow(() -> new EntityNotFoundException("Editor " + id));
    }
}
