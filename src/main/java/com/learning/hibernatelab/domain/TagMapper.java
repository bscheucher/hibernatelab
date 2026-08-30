package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Tag;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(uses = RefMapper.class)
public interface TagMapper {
    TagView toView(Tag tag);
    List<TagView> toViews(List<Tag> tags);
}
