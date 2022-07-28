create table if not exists projects(
    id BIGSERIAL PRIMARY KEY,
    name TEXT UNIQUE,
    author UUID references users(id),
    created timestamp with time zone,
    deleted timestamp with time zone
);