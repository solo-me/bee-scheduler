# DROP TABLE IF EXISTS BS_FIRED_TRIGGERS;
# DROP TABLE IF EXISTS BS_PAUSED_TRIGGER_GRPS;
# DROP TABLE IF EXISTS BS_SCHEDULER_STATE;
# DROP TABLE IF EXISTS BS_LOCKS;
# DROP TABLE IF EXISTS BS_SIMPLE_TRIGGERS;
# DROP TABLE IF EXISTS BS_SIMPROP_TRIGGERS;
# DROP TABLE IF EXISTS BS_CRON_TRIGGERS;
# DROP TABLE IF EXISTS BS_BLOB_TRIGGERS;
# DROP TABLE IF EXISTS BS_TRIGGERS;
# DROP TABLE IF EXISTS BS_JOB_DETAILS;
# DROP TABLE IF EXISTS BS_CALENDARS;
# DROP TABLE IF EXISTS BS_TASK_HISTORY;

BEGIN TRANSACTION;

CREATE TABLE BS_JOB_DETAILS(
  SCHED_NAME VARCHAR(120) NOT NULL,
  JOB_NAME VARCHAR(200) NOT NULL,
  JOB_GROUP VARCHAR(200) NOT NULL,
  DESCRIPTION VARCHAR(250) NULL,
  JOB_CLASS_NAME VARCHAR(250) NOT NULL,
  IS_DURABLE VARCHAR(1) NOT NULL,
  IS_NONCONCURRENT VARCHAR(1) NOT NULL,
  IS_UPDATE_DATA VARCHAR(1) NOT NULL,
  REQUESTS_RECOVERY VARCHAR(1) NOT NULL,
  JOB_DATA BLOB NULL,
  PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP))
  ENGINE=InnoDB;

CREATE TABLE BS_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  JOB_NAME VARCHAR(200) NOT NULL,
  JOB_GROUP VARCHAR(200) NOT NULL,
  DESCRIPTION VARCHAR(250) NULL,
  NEXT_FIRE_TIME BIGINT(13) NULL,
  PREV_FIRE_TIME BIGINT(13) NULL,
  PRIORITY INTEGER NULL,
  TRIGGER_STATE VARCHAR(16) NOT NULL,
  TRIGGER_TYPE VARCHAR(8) NOT NULL,
  START_TIME BIGINT(13) NOT NULL,
  END_TIME BIGINT(13) NULL,
  CALENDAR_NAME VARCHAR(200) NULL,
  MISFIRE_INSTR SMALLINT(2) NULL,
  JOB_DATA BLOB NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
  REFERENCES BS_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP))
  ENGINE=InnoDB;

CREATE TABLE BS_SIMPLE_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  REPEAT_COUNT BIGINT(7) NOT NULL,
  REPEAT_INTERVAL BIGINT(12) NOT NULL,
  TIMES_TRIGGERED BIGINT(10) NOT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
  REFERENCES BS_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE BS_CRON_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  CRON_EXPRESSION VARCHAR(120) NOT NULL,
  TIME_ZONE_ID VARCHAR(80),
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
  REFERENCES BS_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE BS_SIMPROP_TRIGGERS
(
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  STR_PROP_1 VARCHAR(512) NULL,
  STR_PROP_2 VARCHAR(512) NULL,
  STR_PROP_3 VARCHAR(512) NULL,
  INT_PROP_1 INT NULL,
  INT_PROP_2 INT NULL,
  LONG_PROP_1 BIGINT NULL,
  LONG_PROP_2 BIGINT NULL,
  DEC_PROP_1 NUMERIC(13,4) NULL,
  DEC_PROP_2 NUMERIC(13,4) NULL,
  BOOL_PROP_1 VARCHAR(1) NULL,
  BOOL_PROP_2 VARCHAR(1) NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
  REFERENCES BS_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE BS_BLOB_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  BLOB_DATA BLOB NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
  INDEX (SCHED_NAME,TRIGGER_NAME, TRIGGER_GROUP),
  FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
  REFERENCES BS_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE BS_CALENDARS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  CALENDAR_NAME VARCHAR(200) NOT NULL,
  CALENDAR BLOB NOT NULL,
  PRIMARY KEY (SCHED_NAME,CALENDAR_NAME))
  ENGINE=InnoDB;

CREATE TABLE BS_PAUSED_TRIGGER_GRPS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP))
  ENGINE=InnoDB;

CREATE TABLE BS_FIRED_TRIGGERS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  ENTRY_ID VARCHAR(95) NOT NULL,
  TRIGGER_NAME VARCHAR(200) NOT NULL,
  TRIGGER_GROUP VARCHAR(200) NOT NULL,
  INSTANCE_NAME VARCHAR(200) NOT NULL,
  FIRED_TIME BIGINT(13) NOT NULL,
  SCHED_TIME BIGINT(13) NOT NULL,
  PRIORITY INTEGER NOT NULL,
  STATE VARCHAR(16) NOT NULL,
  JOB_NAME VARCHAR(200) NULL,
  JOB_GROUP VARCHAR(200) NULL,
  IS_NONCONCURRENT VARCHAR(1) NULL,
  REQUESTS_RECOVERY VARCHAR(1) NULL,
  PRIMARY KEY (SCHED_NAME,ENTRY_ID))
  ENGINE=InnoDB;

CREATE TABLE BS_SCHEDULER_STATE (
  SCHED_NAME VARCHAR(120) NOT NULL,
  INSTANCE_NAME VARCHAR(200) NOT NULL,
  LAST_CHECKIN_TIME BIGINT(13) NOT NULL,
  CHECKIN_INTERVAL BIGINT(13) NOT NULL,
  PRIMARY KEY (SCHED_NAME,INSTANCE_NAME))
  ENGINE=InnoDB;

CREATE TABLE BS_LOCKS (
  SCHED_NAME VARCHAR(120) NOT NULL,
  LOCK_NAME VARCHAR(40) NOT NULL,
  PRIMARY KEY (SCHED_NAME,LOCK_NAME))
  ENGINE=InnoDB;

CREATE TABLE BS_TASK_HISTORY (
  SCHED_NAME VARCHAR(120) NOT NULL,
  INSTANCE_ID VARCHAR(200) NOT NULL,
  FIRE_ID VARCHAR(95) NOT NULL,
  TASK_NAME VARCHAR(200) NULL,
  TASK_GROUP VARCHAR(200) NULL,
  FIRED_TIME BIGINT(13) NULL,
  FIRED_WAY VARCHAR(8) NULL,
  COMPLETE_TIME BIGINT(13) NULL,
  EXPEND_TIME BIGINT(13) NULL,
  REFIRED INT NULL,
  EXEC_STATE VARCHAR(10) NULL,
  LOG TEXT NULL,
  PRIMARY KEY (FIRE_ID)
)ENGINE=InnoDB;

CREATE TABLE BS_USER(
	ACCOUNT VARCHAR(50) PRIMARY KEY,
	PWD VARCHAR(50) NOT NULL,
	NAME VARCHAR(100) NULL,
	GMT_CREATE DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
	GMT_MODIFIED DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP
)ENGINE=InnoDB;

CREATE INDEX IDX_BS_J_REQ_RECOVERY ON BS_JOB_DETAILS(SCHED_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_BS_J_GRP ON BS_JOB_DETAILS(SCHED_NAME,JOB_GROUP);

CREATE INDEX IDX_BS_T_J ON BS_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_BS_T_JG ON BS_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_BS_T_C ON BS_TRIGGERS(SCHED_NAME,CALENDAR_NAME);
CREATE INDEX IDX_BS_T_G ON BS_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_BS_T_STATE ON BS_TRIGGERS(SCHED_NAME,TRIGGER_STATE);
CREATE INDEX IDX_BS_T_N_STATE ON BS_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_BS_T_N_G_STATE ON BS_TRIGGERS(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
CREATE INDEX IDX_BS_T_NEXT_FIRE_TIME ON BS_TRIGGERS(SCHED_NAME,NEXT_FIRE_TIME);
CREATE INDEX IDX_BS_T_NFT_ST ON BS_TRIGGERS(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
CREATE INDEX IDX_BS_T_NFT_MISFIRE ON BS_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
CREATE INDEX IDX_BS_T_NFT_ST_MISFIRE ON BS_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
CREATE INDEX IDX_BS_T_NFT_ST_MISFIRE_GRP ON BS_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

CREATE INDEX IDX_BS_FT_TRIG_INST_NAME ON BS_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME);
CREATE INDEX IDX_BS_FT_INST_JOB_REQ_RCVRY ON BS_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
CREATE INDEX IDX_BS_FT_J_G ON BS_FIRED_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
CREATE INDEX IDX_BS_FT_JG ON BS_FIRED_TRIGGERS(SCHED_NAME,JOB_GROUP);
CREATE INDEX IDX_BS_FT_T_G ON BS_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
CREATE INDEX IDX_BS_FT_TG ON BS_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);

CREATE INDEX IDX_SCHED_NAME ON BS_TASK_HISTORY(SCHED_NAME);
CREATE INDEX IDX_TASK_NAME ON BS_TASK_HISTORY(TASK_NAME);
CREATE INDEX IDX_TASK_GROUP ON BS_TASK_HISTORY(TASK_GROUP);
CREATE INDEX IDX_FIRED_WAY ON BS_TASK_HISTORY(FIRED_WAY);

INSERT INTO BS_USER(ACCOUNT, PWD, NAME) VALUES('admin','admin','管理员');

COMMIT TRANSACTION;