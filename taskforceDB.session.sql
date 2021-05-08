select id,
    name,
    author,
    created,
    deleted,
    (
        select sum(duration)
        from tasks
        where project_id = p.id
            and deleted is null
    )
from projects p