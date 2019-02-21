--定义数据表

--
CREATE TABLE user_info(
  user_id VARCHAR(128) primary key not null ,
  mail VARCHAR(256) not null ,
  password VARCHAR(256) not null ,
  create_time bigint default 0 not null ,
  state int default 1 not null
)