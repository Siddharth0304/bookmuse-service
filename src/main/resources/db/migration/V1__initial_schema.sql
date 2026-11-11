create table users (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    name varchar(255) not null,
    bio varchar(255),
    avatar_url varchar(255),
    favourite_genres text not null,
    role varchar(255) not null,
    blocked boolean not null
);

create table books (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    title varchar(255) not null,
    author varchar(255) not null,
    genres text not null,
    summary text,
    cover_url varchar(255)
);

create table library_entries (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    book_id uuid not null,
    status varchar(255) not null,
    progress integer
);

create table posts (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    book_id uuid not null,
    body text not null
);

create table post_likes (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    post_id uuid not null
);

create table shares (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    post_id uuid not null
);

create table comments (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    post_id uuid not null,
    body text not null
);

create table follows (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    follower_id uuid not null,
    following_id uuid not null
);

create table clubs (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    creator_id uuid not null,
    name varchar(255) not null,
    description text
);

create table memberships (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    club_id uuid not null
);

create table discussions (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    club_id uuid not null,
    title varchar(255) not null,
    body text not null
);

create table replies (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    discussion_id uuid not null,
    body text not null
);

create table notifications (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    actor_id uuid not null,
    type varchar(255) not null,
    resource_id uuid not null,
    read boolean not null
);

create table refresh_tokens (
    id uuid primary key,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    user_id uuid not null,
    token_hash varchar(255) not null,
    expires_at timestamp(6) with time zone not null
);

alter table library_entries add foreign key (user_id) references users(id) on delete cascade;

create index ix_library_entries_user_id on library_entries(user_id);

alter table library_entries add foreign key (book_id) references books(id) on delete cascade;

create index ix_library_entries_book_id on library_entries(book_id);

alter table posts add foreign key (user_id) references users(id) on delete cascade;

create index ix_posts_user_id on posts(user_id);

alter table posts add foreign key (book_id) references books(id) on delete cascade;

create index ix_posts_book_id on posts(book_id);

alter table post_likes add foreign key (user_id) references users(id) on delete cascade;

create index ix_post_likes_user_id on post_likes(user_id);

alter table post_likes add foreign key (post_id) references posts(id) on delete cascade;

create index ix_post_likes_post_id on post_likes(post_id);

alter table shares add foreign key (user_id) references users(id) on delete cascade;

create index ix_shares_user_id on shares(user_id);

alter table shares add foreign key (post_id) references posts(id) on delete cascade;

create index ix_shares_post_id on shares(post_id);

alter table comments add foreign key (user_id) references users(id) on delete cascade;

create index ix_comments_user_id on comments(user_id);

alter table comments add foreign key (post_id) references posts(id) on delete cascade;

create index ix_comments_post_id on comments(post_id);

alter table follows add foreign key (follower_id) references users(id) on delete cascade;

create index ix_follows_follower_id on follows(follower_id);

alter table follows add foreign key (following_id) references users(id) on delete cascade;

create index ix_follows_following_id on follows(following_id);

alter table clubs add foreign key (creator_id) references users(id) on delete cascade;

create index ix_clubs_creator_id on clubs(creator_id);

alter table memberships add foreign key (user_id) references users(id) on delete cascade;

create index ix_memberships_user_id on memberships(user_id);

alter table memberships add foreign key (club_id) references clubs(id) on delete cascade;

create index ix_memberships_club_id on memberships(club_id);

alter table discussions add foreign key (user_id) references users(id) on delete cascade;

create index ix_discussions_user_id on discussions(user_id);

alter table discussions add foreign key (club_id) references clubs(id) on delete cascade;

create index ix_discussions_club_id on discussions(club_id);

alter table replies add foreign key (user_id) references users(id) on delete cascade;

create index ix_replies_user_id on replies(user_id);

alter table replies add foreign key (discussion_id) references discussions(id) on delete cascade;

create index ix_replies_discussion_id on replies(discussion_id);

alter table notifications add foreign key (user_id) references users(id) on delete cascade;

create index ix_notifications_user_id on notifications(user_id);

alter table notifications add foreign key (actor_id) references users(id) on delete cascade;

create index ix_notifications_actor_id on notifications(actor_id);

alter table refresh_tokens add foreign key (user_id) references users(id) on delete cascade;

create index ix_refresh_tokens_user_id on refresh_tokens(user_id);

alter table users add constraint uq_users_email unique(email);

alter table users add check (email=lower(email));

alter table users add check (role in ('ADMIN','USER'));

create unique index only_one_admin on users(role) where role='ADMIN';

alter table users add check (not (role='ADMIN' and blocked));

alter table library_entries add unique(user_id,book_id);

alter table library_entries add check (status in ('WANT_TO_READ','READING','READ'));

alter table library_entries add check (progress between 0 and 100);

alter table post_likes add unique(user_id,post_id);

alter table follows add unique(follower_id,following_id);

alter table follows add check(follower_id<>following_id);

alter table memberships add unique(user_id,club_id);

alter table refresh_tokens add unique(token_hash);

create index ix_refresh_expiry on refresh_tokens(expires_at);

create index ix_notifications_unread on notifications(user_id, read, created_at desc);

create index ix_books_title on books(lower(title));

create index ix_books_author on books(lower(author));

create index ix_posts_created on posts(created_at desc);

create index ix_clubs_created on clubs(created_at desc);

create index ix_discussions_created on discussions(club_id,created_at desc);

create index ix_replies_created on replies(discussion_id,created_at);
