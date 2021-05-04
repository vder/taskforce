create table if not exists tasks(
    id TEXT PRIMARY KEY,
    project_id int NOT NULL REFERENCES projects(id),
    author UUID references users(id),
    started timestamp without time zone,
    duration Int,
    volume int,
    deleted timestamp without time zone,
    comment text
);