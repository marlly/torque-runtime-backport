-- ------------------------------------------------------------------------
-- Simple script to migrate a postgresql id_table from the
-- old column names to the new ones, prefixed with id_table
-- ------------------------------------------------------------------------
-- Usage: psql <options> yourdatabase < migration-postgresql.sql
-- ------------------------------------------------------------------------
-- @Author:  Henning P. Schmiedehausen <hps@intermeta.de>
-- @Version: $Header$
-- ------------------------------------------------------------------------

alter table id_table add column id_table_table_name varchar(255);
alter table id_table add column id_table_next_id integer;        
alter table id_table add column id_table_quantity integer;

update id_table set id_table_table_name = table_name;
update id_table set id_table_next_id = next_id;      
update id_table set id_table_quantity = quantity;

alter table id_table alter column id_table_table_name set not null;
alter table id_table alter column id_table_next_id set not null;
alter table id_table alter column id_table_quantity set not null;

alter table id_table drop table_name;                            
alter table id_table drop next_id;   
alter table id_table drop quantity;
