-- Integrity hardening for the many-to-many join tables.
--
-- 1. ON DELETE CASCADE so a book/author/editor/tag can be deleted without first
--    manually clearing its link rows (a plain reference would raise a FK error).
-- 2. Unique constraints on the lookup names to prevent duplicates.
-- 3. Indexes on the trailing join column: the composite PKs only index the
--    leading column, so reverse lookups (e.g. authors of a book) and the
--    cascade FK checks were unindexed.

-- 1. Cascade deletes -----------------------------------------------------------

ALTER TABLE author_book
    DROP CONSTRAINT author_book_author_id_fkey,
    ADD  CONSTRAINT author_book_author_id_fkey
        FOREIGN KEY (author_id) REFERENCES author (id) ON DELETE CASCADE,
    DROP CONSTRAINT author_book_book_id_fkey,
    ADD  CONSTRAINT author_book_book_id_fkey
        FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE;

ALTER TABLE editor_book
    DROP CONSTRAINT editor_book_editor_id_fkey,
    ADD  CONSTRAINT editor_book_editor_id_fkey
        FOREIGN KEY (editor_id) REFERENCES editor (id) ON DELETE CASCADE,
    DROP CONSTRAINT editor_book_book_id_fkey,
    ADD  CONSTRAINT editor_book_book_id_fkey
        FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE;

ALTER TABLE book_tag
    DROP CONSTRAINT book_tag_book_id_fkey,
    ADD  CONSTRAINT book_tag_book_id_fkey
        FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE,
    DROP CONSTRAINT book_tag_tag_id_fkey,
    ADD  CONSTRAINT book_tag_tag_id_fkey
        FOREIGN KEY (tag_id) REFERENCES tag (id) ON DELETE CASCADE;

-- 2. Unique lookup names -------------------------------------------------------

ALTER TABLE tag    ADD CONSTRAINT tag_name_unique    UNIQUE (name);
ALTER TABLE author ADD CONSTRAINT author_name_unique UNIQUE (name);
ALTER TABLE editor ADD CONSTRAINT editor_name_unique UNIQUE (name);

-- 3. Reverse-direction indexes on the join tables -----------------------------

CREATE INDEX idx_author_book_book ON author_book (book_id);
CREATE INDEX idx_editor_book_book ON editor_book (book_id);
CREATE INDEX idx_book_tag_tag     ON book_tag (tag_id);
