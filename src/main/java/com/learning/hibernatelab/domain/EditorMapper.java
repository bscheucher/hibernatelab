package com.learning.hibernatelab.domain;

import com.learning.hibernatelab.persistence.Editor;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(uses = RefMapper.class)
public interface EditorMapper {
    EditorView toView(Editor editor);
    List<EditorView> toViews(List<Editor> editors);
}
