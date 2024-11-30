create table if not exists tasks(
    id UUID PRIMARY KEY,
    project_id int NOT NULL REFERENCES projects(id),
    author UUID references users(id),
    started timestamp with time zone,
    duration Int,
    volume int,
    deleted timestamp with time zone,
    comment text
);