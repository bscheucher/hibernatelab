package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Publisher;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(uses = RefMapper.class)
public interface PublisherMapper {
    PublisherView toView(Publisher publisher);
    List<PublisherView> toViews(List<Publisher> publishers);
}
