create table if not exists projects(
    id BIGSERIAL PRIMARY KEY,
    name TEXT UNIQUE,
    author UUID references users(id),
    created timestamp without time zone,
    deleted timestamp without time zone
);