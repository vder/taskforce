delete from tasks;
delete from projects;
insert into projects(name, author, created)
values(
        'project 1',
        '5260ca29-a70b-494e-a3d6-55374a3b0a04',
        '2021-05-09 13:38:17.730944'
    );
insert into projects(name, author, created)
values(
        'project 2',
        '5260ca29-a70b-494e-a3d6-55374a3b0a04',
        '2021-05-09 13:38:17.730944'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        '54b28d5e-3b33-46a6-a02c-4ac8159a7bcd',
        1,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '0001-04-17 01:35:32',
        10,
        16,
        null,
        'task11'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        'ba386e57-c609-4703-9e0b-18428b41c84f',
        1,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '0002-08-19 09:53:30',
        1000,
        51,
        null,
        'task12'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        'e274078c-21d8-44c2-b832-05e484828c6e',
        1,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '0003-07-02 00:00:01',
        1000,
        5,
        null,
        'task13'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        'd2e3a5eb-29dc-4a6c-aef5-372ecf55592a',
        1,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '0004-07-28 23:27:20',
        1000,
        11,
        null,
        'task14'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        '18231054-b3e8-4d57-bfb1-6c3b6b9923d0',
        1,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '0005-07-04 23:23:39',
        1000,
        27,
        '0005-07-04 23:23:39',
        'task15'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        'bd9274f8-c00f-4838-8f64-8e42f17cbbc3',
        1,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '0006-07-02 00:00:00',
        10,
        16,
        '0006-07-02 00:00:00',
        'task16'
    );
----
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        '721a73c4-08ad-4cba-96d8-6d295cd60ecb',
        2,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '0201-07-02 00:00:01',
        1000,
        52,
        null,
        'task21'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        'b22d9602-c8e1-47b7-a2d7-7e01ef933fef',
        2,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '0202-03-10 01:46:40',
        995,
        84,
        null,
        'task22'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        '8ea8ea18-93bc-4ca7-b213-661d91f9f339',
        2,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '203-11-24 11:40:56',
        452,
        6,
        null,
        'task23'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        'bf274431-73fa-40ef-b2bc-03bdd5caa851',
        2,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '204-07-02 00:00:01',
        409,
        22,
        null,
        'task24'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        '9d586225-6667-48ac-8076-6460cd64e35e',
        2,
        'aa7e1d66-a386-11eb-bcbc-0242ac130002',
        '205-07-02 00:00:01',
        877,
        97,
        '205-07-02 00:00:01',
        'task25'
    );
insert into tasks(
        id,
        project_id,
        author,
        started,
        duration,
        volume,
        deleted,
        comment
    )
values(
        'a43be0db-1e32-410e-abd6-c903c5459b18',
        2,
        '5260ca29-a70b-494e-a3d6-55374a3b0a04',
        '206-11-22 02:02:20',
        1000,
        98,
        '206-11-22 02:02:20',
        'task26'
    );