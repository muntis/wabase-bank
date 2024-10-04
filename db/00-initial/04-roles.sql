
insert into adm_role(id, name, code, notes, is_system)
values (nextval('seq'), 'Admin', 'admin', 'System administrator', false);

insert into adm_permission(id, code, comment)
values (nextval('seq'), 'MANAGE_CLASSIFIERS', 'Manage classifiers');


insert into adm_role_permission(id, adm_role_id, adm_permission_id)
select nextval('seq'), r.id, p.id
from adm_role r, adm_permission p
where r.code = 'admin' and p.code = 'MANAGE_CLASSIFIERS';


