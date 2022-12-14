create table if not exists telegram_user
(
    id         bigint       not null primary key,
    full_name  varchar(255) not null,
    username   varchar(255),
    last_topic int,
    active     boolean      not null default true
);

create table if not exists channel
(
    id       bigint not null primary key,
    group_id bigint not null unique
);

create table if not exists channel_user
(
    id         int    not null generated by default as identity primary key,
    user_id    bigint not null,
    channel_id bigint not null,
    foreign key (user_id) references telegram_user (id) on delete cascade,
    foreign key (channel_id) references channel (id) on delete cascade
);

create unique index ui_channel_user
    on channel_user (channel_id, user_id);

create table if not exists topic
(
    id                int    not null generated by default as identity primary key,
    channel_id        bigint not null,
    channel_thread_id bigint not null,
    group_thread_id   bigint not null,
    foreign key (channel_id) references channel (id)
);

alter table telegram_user
    add foreign key (last_topic) references topic (id);

create table if not exists topic_user
(
    id       int    not null generated by default as identity primary key,
    topic_id int    not null,
    user_id  bigint not null,
    foreign key (topic_id) references topic (id) on delete cascade
);

create unique index ui_topic_user
    on topic_user (topic_id, user_id);

create table if not exists user_history
(
    id                                  int    not null generated by default as identity primary key,
    user_id                             bigint not null,
    topic_id                            int    not null,
    unread_messages_count               int    not null default 0,
    unread_count_message_id             int,
    topic_notification_message_id       int,
    owner_notification_message_id       int,
    owner_original_message_id           int,
    notification_message_id             int,
    original_message_id                 int,
    notification_message_with_button_id int,
    foreign key (user_id) references telegram_user (id),
    foreign key (topic_id) references topic (id)
);

create unique index ui_user_history
    on user_history (topic_id, user_id);