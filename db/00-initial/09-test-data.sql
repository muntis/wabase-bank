-- ADMINISTRATOR
insert into adm_user(id, status, email, notes, passwd,name, surname, phone, position, pk, password_expiration_date) values
(nextval('seq'), 'Active', 'admin@localhost', 'System administrator', '$s0$e0801$d8nb1HWjHdbFi9z5JerbCA==$u7tt8tN0lzCv9p0Vkq5FczlbRjZLMoSaCwq1aFCVQew=', 'Admin', '1234', '+371', 'Admin', '12345', now() + interval '1 year');

insert into adm_user_role(id, adm_user_id, adm_role_id, date_from)
select nextval('seq'), u.id, r.id, now()
from adm_user u, adm_role r
where u.email = 'admin@localhost' and r.code = 'admin';


insert into classifier(id, code, name, notes, is_hierarchical, is_active, parent_id) values
(nextval('seq'), 'TEST', 'Test classifier', 'Test classifier', false, true, null);

insert into classifier_item(id, parent_id, classifier_id, code, name, date_from, date_to, notes, longname, code_sort, code_postdoc)
select nextval('seq'), null, c.id, 'TESTREC', 'Test classifier item', now(), null, 'Test classifier item', 'Test classifier item', 'TEST', 'TEST'
from classifier c
where c.code = 'TEST';

