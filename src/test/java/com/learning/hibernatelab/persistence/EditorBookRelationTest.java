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
class EditorBookRelationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EditorRepository editorRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void addEditorToBook() {
        // Unique names so the test doesn't collide with any data already in the
        // shared database (editor.name is unique as of migration V4).
        String suffix = UUID.randomUUID().toString();
        String bookTitle = "Effective Java " + suffix;
        String editorName = "Addison-Wesley " + suffix;

        Book book = new Book();
        book.setTitle(bookTitle);
        book = bookRepository.save(book);

        Editor editor = new Editor();
        editor.setName(editorName);
        // Editor is the owning side of the many-to-many, so the link is set here.
        editor.getBooks().add(book);
        Editor savedEditor = editorRepository.save(editor);

        // Flush the insert into editor_book and drop the first-level cache so the
        // reload below actually hits the database instead of returning cached refs.
        entityManager.flush();
        entityManager.clear();

        Book reloaded = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(reloaded.getEditors())
                .extracting(Editor::getName)
                .containsExactly(editorName);

        Editor reloadedEditor = editorRepository.findById(savedEditor.getId()).orElseThrow();
        assertThat(reloadedEditor.getBooks())
                .extracting(Book::getTitle)
                .containsExactly(bookTitle);
    }
}
