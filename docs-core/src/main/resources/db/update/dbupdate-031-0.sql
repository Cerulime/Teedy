-- DBUPDATE-031-0.SQL
create table T_LOGIN_REQUEST ( LR_ID_C varchar(36) not null, LR_TOKEN_C varchar(100) not null, LR_IP_C varchar(45) not null, LR_TIMESTAMP_D datetime not null, LR_STATUS_C varchar(20) not null, primary key (LR_ID_C) );

-- Insert a new setting for OCR recognition
insert into T_CONFIG (CFG_ID_C, CFG_VALUE_C) values ('OCR_ENABLED', 'true');

-- Update the database version
update T_CONFIG set CFG_VALUE_C = '31' where CFG_ID_C = 'DB_VERSION';
