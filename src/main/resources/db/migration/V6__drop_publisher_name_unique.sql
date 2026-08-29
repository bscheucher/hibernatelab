-- A publisher's name is not a reliable natural key: two distinct publishing
-- houses can share a name (different jurisdictions, reuse after one folds,
-- spun-off imprints). A real identifier would be a registration number or an
-- ISBN publisher prefix, not the display name. So drop the uniqueness.
--
-- Tag keeps its unique name: a tag is a controlled label whose name *is* its
-- identity, not a real-world entity that merely has a name.
ALTER TABLE publisher DROP CONSTRAINT publisher_name_unique;
