-- Table backing Spring Modulith's JPA event publication registry
-- (spring-modulith-starter-jpa). Kept in Flyway so Hibernate's schema
-- validation is satisfied and the schema stays under version control.
create table event_publication (
    id                     uuid                        not null,
    listener_id            text                        not null,
    event_type             text                        not null,
    serialized_event       text                        not null,
    publication_date       timestamp(6) with time zone not null,
    completion_date        timestamp(6) with time zone,
    last_resubmission_date timestamp(6) with time zone,
    completion_attempts    integer                     not null,
    status                 varchar(255),
    primary key (id)
);
