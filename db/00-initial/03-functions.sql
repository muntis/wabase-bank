create or replace function checked_resolve(resolvable text, resolved bigint[], error_message text)
  returns bigint as $$
begin
  if array_length(resolved, 1) > 1 or resolvable is not null and resolved[1] is null then
    raise exception sqlstate '235BX' using message = error_message;
  else
    return resolved[1];
  end if;
end;
$$ language plpgsql immutable;

create or replace function checked_resolve(resolvable text, resolved text[], error_message text)
  returns text as $$
begin
  if array_length(resolved, 1) > 1 or resolvable is not null and resolved[1] is null then
    raise exception sqlstate '235BX' using message = error_message;
  else
    return resolved[1];
  end if;
end;
$$ language plpgsql immutable;

create or replace function checked_resolve(resolvable text, resolved date[], error_message text)
  returns date as $$
begin
  if array_length(resolved, 1) > 1 or resolvable is not null and resolved[1] is null then
    raise exception sqlstate '235BX' using message = error_message;
  else
    return resolved[1];
  end if;
end;
$$ language plpgsql immutable;

create extension unaccent;

create or replace function f_unaccent(text)
  returns text as
$func$
select public.unaccent('unaccent', $1)
$func$ language sql immutable set search_path = public, pg_temp;

