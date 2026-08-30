package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Tag;
import com.learning.hibernatelab.persistence.TagRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reads and maintains tags.
 *
 * <p>There is deliberately no {@code addBook} here. {@code book_tag} is owned by
 * {@link com.learning.hibernatelab.persistence.Book}, so tagging a book is
 * {@link BookService#addTag}; a write through {@code tag.getBooks()} would be
 * discarded at flush.
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Transactional(readOnly = true)
    public List<TagView> findAll() {
        return tagMapper.toViews(tagRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<TagView> findById(Long id) {
        return tagRepository.findWithBooksById(id).map(tagMapper::toView);
    }

    @Transactional(readOnly = true)
    public Optional<TagView> findByName(String name) {
        return tagRepository.findByName(name).map(tagMapper::toView);
    }

    /**
     * Returns the existing tag with this name, or creates it.
     *
     * <p>{@code tag.name} is unique (migration {@code V4}), so this is the safe
     * way to reach a tag by name without tripping the constraint.
     */
    @Transactional
    public TagView findOrCreate(String name) {
        Tag tag = tagRepository.findByName(name).orElseGet(() -> {
            Tag created = new Tag();
            created.setName(name);
            return tagRepository.save(created);
        });
        return tagMapper.toView(tag);
    }

    /**
     * Deletes a tag, dropping it from every book that carried it.
     *
     * <p>Tag is the inverse side of {@code book_tag}, so Hibernate does not
     * clear the join rows itself. They go because migration {@code V4} declared
     * {@code ON DELETE CASCADE} on the table.
     */
    @Transactional
    public void delete(Long id) {
        tagRepository.delete(tagRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tag " + id)));
    }
}
