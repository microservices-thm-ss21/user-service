CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

create table users (
    id uuid primary key DEFAULT uuid_generate_v4(),
    username varchar(50) unique,
    password varchar(100),
    name varchar(50),
    last_name varchar(50),
    email varchar(50),
    date_of_birth date,
    create_time timestamp,
    global_role varchar(20),
    last_login timestamp null
);

-- the following users all have the password "password" hashed to "{bcrypt}$2a$10$t.3KubtyXsOEO9.eoWRzTOZMMTuiPvLrhSe9YFzgzV2J8kVtIOiLa"
insert into users values ('a443ffd0-f7a8-44f6-8ad3-87acd1e91042', 'Peter_Zwegat', '{bcrypt}$2a$10$t.3KubtyXsOEO9.eoWRzTOZMMTuiPvLrhSe9YFzgzV2J8kVtIOiLa', 'Peter', 'Zwegat', 'peter.zwegat@mni.thm.de', current_date, current_timestamp, 'ADMIN', current_timestamp);
insert into users values ('a443ffd0-f7a8-44f6-8ad3-87acd1e91043', 'Kim-Jong-Dos', '{bcrypt}$2a$10$t.3KubtyXsOEO9.eoWRzTOZMMTuiPvLrhSe9YFzgzV2J8kVtIOiLa', 'Kim', 'Jong-Dos', 'kim@nord-korea.com', current_date, current_timestamp, 'USER', current_timestamp);
insert into users values ('a443ffd0-f7a8-44f6-8ad3-87acd1e91044', 'Kim-Jong-On', '{bcrypt}$2a$10$t.3KubtyXsOEO9.eoWRzTOZMMTuiPvLrhSe9YFzgzV2J8kVtIOiLa', 'Kim', 'Jong-On', 'lil-kim@nord-korea.com', current_date, current_timestamp, 'USER', current_timestamp)
