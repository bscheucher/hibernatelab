package com.learning.hibernatelab.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
        Book book = new Book();
        book.setTitle("Effective Java");
        book = bookRepository.save(book);

        Editor editor = new Editor();
        editor.setName("Addison-Wesley");
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
                .containsExactly("Addison-Wesley");

        Editor reloadedEditor = editorRepository.findById(savedEditor.getId()).orElseThrow();
        assertThat(reloadedEditor.getBooks())
                .extracting(Book::getTitle)
                .containsExactly("Effective Java");
    }
}
