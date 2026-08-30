package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Publisher;
import com.learning.hibernatelab.persistence.PublisherRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reads and maintains publishers (Verlage).
 *
 * <p>Like {@link TagService}, this service has no {@code addBook}. The
 * {@code book.publisher_id} foreign key is owned by
 * {@link com.learning.hibernatelab.persistence.Book}, so assigning a publisher
 * is {@link BookService#setPublisher}; writing through
 * {@code publisher.getBooks()} would be discarded at flush.
 */
@Service
@RequiredArgsConstructor
public class PublisherService {

    private final PublisherRepository publisherRepository;
    private final PublisherMapper publisherMapper;

    @Transactional(readOnly = true)
    public List<PublisherView> findAll() {
        return publisherMapper.toViews(publisherRepository.findAll());
    }

    @Transactional(readOnly = true)
    public Optional<PublisherView> findById(Long id) {
        return publisherRepository.findWithBooksById(id).map(publisherMapper::toView);
    }

    /**
     * Finds publishers by name — plural, deliberately.
     *
     * <p>Migration {@code V6} dropped the unique constraint on
     * {@code publisher.name}, so a name identifies a house no better than
     * {@code author.name} identifies a person. Callers that need one row want
     * {@link #findById}.
     */
    @Transactional(readOnly = true)
    public List<PublisherView> findByName(String name) {
        return publisherMapper.toViews(publisherRepository.findByName(name));
    }

    @Transactional
    public PublisherView create(String name) {
        Publisher publisher = new Publisher();
        publisher.setName(name);
        return publisherMapper.toView(publisherRepository.save(publisher));
    }

    @Transactional
    public PublisherView rename(Long id, String name) {
        Publisher publisher = require(id);
        publisher.setName(name);
        return publisherMapper.toView(publisher);
    }

    /**
     * Deletes a publisher, leaving its books in place without one.
     *
     * <p>Publisher is the inverse side of the relation, so Hibernate does not
     * clear {@code book.publisher_id} itself. The books survive because
     * migration {@code V5} declared the foreign key {@code ON DELETE SET NULL} —
     * which is also why {@link BookView#publisher()} is allowed to be null.
     */
    @Transactional
    public void delete(Long id) {
        publisherRepository.delete(require(id));
    }

    private Publisher require(Long id) {
        return publisherRepository.findWithBooksById(id)
                .orElseThrow(() -> new EntityNotFoundException("Publisher " + id));
    }
}
