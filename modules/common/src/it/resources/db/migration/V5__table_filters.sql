create table if not exists filters(
    id SERIAL PRIMARY KEY,
    filter_id UUID,
    criteria_type varchar(20),
    field varchar(20),
    operator varchar(20),
    date_value timestamp without time zone,
    status_value varchar(20),
    list_value text []
);