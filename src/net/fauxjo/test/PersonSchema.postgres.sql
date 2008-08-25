--
-- PersonSchema.postgres.sql
--

create table Department
(
    departmentId bigserial primary key,
    name varchar(500)
);

comment on table Department is 'Department';
comment on column Department.departmentId is 'departmentId';
comment on column Department.name is 'name';



create table Person
(
    personId varchar(500) primary key,
    firstName varchar(500),
    lastName varchar(500),
    address varchar(500),
    entryDate date,
    entryTimestamp timestamp,
    departmentId bigint,
    foreign key (departmentId) references Department(departmentId)
);

comment on table Person is 'Person';
comment on column Person.personId is 'personId';
comment on column Person.firstName is 'firstName';
comment on column Person.lastName is 'lastName';
comment on column Person.address is 'address';
comment on column Person.entryDate is 'entryDate';
comment on column Person.entryTimestamp is 'entryTimestamp';
comment on column Person.departmentId is 'departmentId';



create table MonkeyMan
(
   monkeyId bigint primary key,
   name varchar(1000),
   shaved boolean,
   numFingers int not null,
   height float,
   birthday date,
   entryDate timestamp,
   depId bigint,
   foreign key (depId) references Department(departmentId)
)

comment on table MonkeyMan is 'MonkeyMan';
comment on column MonkeyMan.monkeyId is 'monkeyId';
comment on column MonkeyMan.name is 'name';
comment on column MonkeyMan.shaved is 'shaved';
comment on column MonkeyMan.numFingers is 'numFingers';
comment on column MonkeyMan.height is 'height';
comment on column MonkeyMan.birthday is 'birthday';
comment on column MonkeyMan.entryDate is 'entryDate';
comment on column MonkeyMan.depId is 'depId';
