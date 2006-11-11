-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.
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
