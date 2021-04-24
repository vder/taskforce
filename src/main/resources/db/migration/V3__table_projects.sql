create table if not exists projects(
    id Serial PRIMARY KEY,
    name TEXT UNIQUE,
    author UUID,
    created timestamp without time zone,
    deleted timestamp without time zone
);