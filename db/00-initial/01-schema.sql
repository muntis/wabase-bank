create table account(
  id bigint,
  client_id bigint not null,
  balance numeric(12, 2) not null,
  currency text not null,
  type text,
  date_created date not null,
  date_closed date
);
comment on table account is 'Bank account data';
comment on column account.client_id is 'Client id';
comment on column account.balance is 'Account balance';
comment on column account.currency is 'Account currency';
comment on column account.type is 'Account type';
comment on column account.date_created is 'Account creation date';
comment on column account.date_closed is 'Account closing date';

create table account_transaction(
  id bigint,
  from_account_id bigint not null,
  to_account_id bigint not null,
  amount numeric(12, 2) not null,
  currency text not null
);
comment on table account_transaction is 'Money transfer transaction';
comment on column account_transaction.from_account_id is 'From account id';
comment on column account_transaction.to_account_id is 'To account id';
comment on column account_transaction.amount is 'Transaction amount';
comment on column account_transaction.currency is 'Transaction currency';

create table adm_password_history(
  id bigint,
  adm_user_id bigint not null,
  passwd varchar(256) not null,
  change_time timestamp
);

create table adm_permission(
  id bigint,
  code varchar(100) not null,
  comment varchar(250)
);
comment on table adm_permission is 'Atomic permissions - edit, delete specific data';

create table adm_role(
  id bigint,
  name varchar(100) not null,
  code varchar(100),
  notes varchar(250),
  is_system bool default false not null
);
comment on table adm_role is 'System roles';

create table adm_role_permission(
  id bigint,
  adm_role_id bigint not null,
  adm_permission_id bigint not null
);
comment on table adm_role_permission is 'Permissions within role';
comment on column adm_role_permission.adm_role_id is 'Link to role';
comment on column adm_role_permission.adm_permission_id is 'Link to permission';

create table adm_user(
  id bigint,
  email text,
  notes varchar(250),
  passwd varchar(256),
  text text,
  name text not null,
  surname text not null,
  phone text,
  position text,
  pk varchar(12),
  failed_login_attempts integer default 0 not null,
  password_expiration_date date not null,
  last_login_time timestamp,
  last_login_ip text,
  last_failed_login_time timestamp,
  last_failed_login_success_ip text,
  status text not null check (status in ('Active', 'Inactive', 'Blocked')),
  activation_code varchar(256),
  is_code_used bool default false
);

create table adm_user_history(
  id bigint,
  adm_user_id bigint not null,
  date_from date default now() not null,
  name text,
  surname text,
  pk varchar(12)
);

create table adm_user_role(
  id bigint,
  adm_user_id bigint not null,
  adm_role_id bigint,
  date_from date not null,
  date_to date
);
comment on table adm_user_role is 'Link between user and role';
comment on column adm_user_role.adm_user_id is 'Link to user';
comment on column adm_user_role.adm_role_id is 'Link to role';
comment on column adm_user_role.date_from is 'Validity period from';
comment on column adm_user_role.date_to is 'Validity period to';

create table audit(
  id bigint,
  time timestamp default now() not null,
  action text not null check (action in ('login', 'logout', 'create', 'save', 'remove', 'open', 'start', 'end')),
  source text,
  status varchar(8) not null check (status in ('success', 'error')),
  entity text,
  entity_id bigint,
  vards text,
  uzvards text,
  adm_user_id bigint,
  epasts text,
  ip_address text,
  user_agent text,
  error_data text,
  json_data text
);
comment on table audit is 'Audit logs for user actions';
comment on column audit.time is 'Record creation time';
comment on column audit.action is 'Action type';
comment on column audit.source is 'Source of the action, PORTAL, API, etc.';
comment on column audit.status is 'Action status';
comment on column audit.entity is 'View name';
comment on column audit.entity_id is 'View ID';
comment on column audit.vards is 'User name';
comment on column audit.uzvards is 'User surname';
comment on column audit.adm_user_id is 'User ID';
comment on column audit.epasts is 'User email';
comment on column audit.ip_address is 'User IP address';
comment on column audit.user_agent is 'User agent';

create table classifier(
  id bigint,
  code text not null,
  name text not null,
  notes varchar(250),
  is_hierarchical bool default false not null,
  is_active bool default true not null,
  parent_id bigint
);

create table classifier_item(
  id bigint,
  parent_id bigint,
  classifier_id bigint,
  code text,
  name text,
  date_from date,
  date_to date,
  notes varchar(2000),
  longname text,
  code_sort text,
  code_postdoc text
);

create table client(
  id bigint,
  name text,
  surname text,
  address text,
  phone text,
  email text,
  date_created date not null,
  date_updated date not null,
  last_login text
);
comment on table client is 'Bank client data';
comment on column client.name is 'Client name';
comment on column client.surname is 'Client surname';
comment on column client.address is 'Client address';
comment on column client.phone is 'Client phone';
comment on column client.email is 'Client email';
comment on column client.date_created is 'Client creation date';
comment on column client.date_updated is 'Client update date';
comment on column client.last_login is 'Client last login date';

create table deferred_file_body_info(
  sha_256 varchar(64),
  size bigint not null,
  path varchar(240) not null
);

create table deferred_file_info(
  id bigint,
  filename varchar(240) not null,
  upload_time timestamp not null,
  content_type varchar(100) not null,
  sha_256 varchar(64) not null
);

create table deferred_request(
  username varchar(50) not null,
  priority integer not null,
  request_time timestamp not null,
  status varchar(5) not null check (status in ('OK', 'ERR', 'QUEUE', 'EXE', 'DEL')),
  topic varchar(50) not null,
  request_hash varchar(100),
  request bytea not null,
  response_time timestamp,
  response_headers bytea,
  response_entity_file_id bigint,
  response_entity_file_sha_256 varchar(64)
);

create table file_body_info(
  sha_256 varchar(64),
  size bigint not null,
  path varchar(240) not null
);

create table file_info(
  id bigint,
  filename varchar(240) not null,
  upload_time timestamp not null,
  content_type varchar(100) not null,
  sha_256 varchar(64) not null
);

create table files_on_disk(
  path varchar(512)
);
comment on table files_on_disk is 'File deletion process service table';
comment on column files_on_disk.path is 'Datnes taka failsistēmā';

alter table account add constraint pk_account primary key (id);

alter table account_transaction add constraint pk_account_transaction primary key (id);

alter table adm_password_history add constraint pk_adm_password_history primary key (id);
create index idx_adm_password_history_adm_user_id on adm_password_history(adm_user_id);

alter table adm_permission add constraint pk_adm_permission primary key (id);

alter table adm_role add constraint pk_adm_role primary key (id);
alter table adm_role add constraint uk_adm_role_code unique(code);
alter table adm_role add constraint uk_adm_role_name unique(name);

alter table adm_role_permission add constraint pk_adm_role_permission primary key (id);
create index idx_adm_role_permission_adm_role_id on adm_role_permission(adm_role_id);
create index idx_adm_role_permission_adm_permission_id on adm_role_permission(adm_permission_id);

alter table adm_user add constraint pk_adm_user primary key (id);
alter table adm_user add constraint uk_adm_user_email unique(email);
alter table adm_user add constraint uk_adm_user_pk unique(pk);

alter table adm_user_history add constraint pk_adm_user_history primary key (id);

alter table adm_user_role add constraint pk_adm_user_role primary key (id);
create index idx_adm_user_role_adm_user_id on adm_user_role(adm_user_id);
create index idx_adm_user_role_adm_role_id on adm_user_role(adm_role_id);

alter table audit add constraint pk_audit primary key (id);

alter table classifier add constraint pk_classifier primary key (id);
create index idx_classifier_is_active on classifier(is_active);
create index idx_classifier_code on classifier(code);

alter table classifier_item add constraint pk_classifier_item primary key (id);
create index idx_classifier_item_classifier_id on classifier_item(classifier_id);
create index idx_classifier_item_code on classifier_item(code);
create index idx_classifier_item_parent_id on classifier_item(parent_id);

alter table client add constraint pk_client primary key (id);

alter table deferred_file_body_info add constraint pk_deferred_file_body_info primary key (sha_256);

alter table deferred_file_info add constraint pk_deferred_file_info primary key (id);

alter table deferred_request add constraint pk_deferred_request primary key (request_hash);
create index idx_deferred_request_priority_request_time on deferred_request(priority, request_time);
create index idx_deferred_request_username on deferred_request(username);

alter table file_body_info add constraint pk_file_body_info primary key (sha_256);

alter table file_info add constraint pk_file_info primary key (id);

alter table files_on_disk add constraint pk_files_on_disk primary key (path);

alter table account add constraint fk_account_client_id foreign key (client_id) references client(id);
alter table account_transaction add constraint fk_account_transaction_from_account_id foreign key (from_account_id) references account(id);
alter table account_transaction add constraint fk_account_transaction_to_account_id foreign key (to_account_id) references account(id);
alter table adm_password_history add constraint fk_adm_password_history_adm_user_id foreign key (adm_user_id) references adm_user(id) on delete cascade;
alter table adm_role_permission add constraint fk_adm_role_permission_adm_role_id foreign key (adm_role_id) references adm_role(id) on delete cascade;
alter table adm_role_permission add constraint fk_adm_role_permission_adm_permission_id foreign key (adm_permission_id) references adm_permission(id) on delete cascade;
alter table adm_user_history add constraint fk_adm_user_history_adm_user_id foreign key (adm_user_id) references adm_user(id);
alter table adm_user_role add constraint fk_adm_user_role_adm_user_id foreign key (adm_user_id) references adm_user(id) on delete cascade;
alter table adm_user_role add constraint fk_adm_user_role_adm_role_id foreign key (adm_role_id) references adm_role(id) on delete cascade;
alter table classifier add constraint fk_classifier_parent_id foreign key (parent_id) references classifier(id);
alter table classifier_item add constraint fk_classifier_item_parent_id foreign key (parent_id) references classifier_item(id);
alter table classifier_item add constraint fk_classifier_item_classifier_id foreign key (classifier_id) references classifier(id);
alter table deferred_file_info add constraint fk_deferred_file_info_sha_256 foreign key (sha_256) references deferred_file_body_info(sha_256);
alter table file_info add constraint fk_file_info_sha_256 foreign key (sha_256) references file_body_info(sha_256);
