select p.id,
       p.name,
       p.author,
       p.created,
       p.deleted,
       t.id,
       t.project_id,
       t.author,
       t.started,
       t.duration,
       t.volume,
       t.deleted,
       t.comment
  from projects p left join tasks t 
    on t.project_id = p.id WHERE (p.name IN ('project 1') )  limit 100 offset 0 