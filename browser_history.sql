create database browser_history

create table if not exists users (
	id serial primary key ,
	username varchar (50) not null,
	password varchar (50) not null
);

create table if not exists Urls (
	id serial primary key,
	value varchar(70) not null,
	timestamp timestamp, 
	userId integer,
	foreign key (userId) references users(id)
);

