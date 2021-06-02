CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

create table projects (
    project_id uuid primary key
);

create table issues (
    issue_id uuid primary key
);

create table users (
    id uuid primary key DEFAULT uuid_generate_v4(),
    username varchar(50),
    name varchar(50),
    last_name varchar(50),
    email varchar(50),
    date_of_birth date,
    create_time timestamp,
    global_role varchar(20),
    last_login timestamp null
);


insert into issues values ('a3974d24-5735-410c-b109-ad262755d4d3');
insert into projects values ('54ed2c8e-054d-4fb0-81ac-d7ed726b1879');
insert into users values ('a443ffd0-f7a8-44f6-8ad3-87acd1e91042', 'Peter_Zwegat', 'Peter', 'Zwegat', 'peter.zwegat@mni.thm.de', current_date, current_timestamp, 'normal', current_timestamp)

