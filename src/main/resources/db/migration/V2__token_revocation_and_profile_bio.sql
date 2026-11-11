alter table users add column token_version integer not null default 0;
alter table users alter column bio type text;
