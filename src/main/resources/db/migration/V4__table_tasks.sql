drop table tasks;
create table if not exists tasks(
    id TEXT PRIMARY KEY,
    project_id int NOT NULL REFERENCES projects,
    author UUID,
    started timestamp without time zone,
    duration Bigint,
    volume int,
    deleted timestamp without time zone,
    comment text
);