-- 核心表补充 BaseEntity 审计字段
-- 适用场景：已经按旧版《正式系统V1数据库建表SQL初稿.sql》建库，后续准备让核心实体继承 BaseEntity。
-- 注意：MySQL 5.x 不支持 ADD COLUMN IF NOT EXISTS。
-- 如果某列已经存在，请不要重复执行对应 ALTER 语句。

-- ad_advertiser 缺少 update_by
ALTER TABLE ad_advertiser
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

-- ad_agent 缺少 update_by
ALTER TABLE ad_agent
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

-- ad_material 缺少 create_by / update_by
ALTER TABLE ad_material
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER uploader_id;

ALTER TABLE ad_material
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

-- ad_plan 缺少 create_by / update_by
ALTER TABLE ad_plan
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER create_user_id;

ALTER TABLE ad_plan
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

-- ad_building 缺少 create_by / update_by
ALTER TABLE ad_building
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER status;

ALTER TABLE ad_building
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

-- ad_device 缺少 create_by / update_by
ALTER TABLE ad_device
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER status;

ALTER TABLE ad_device
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

-- ad_work_order 缺少 create_by / update_by / deleted
ALTER TABLE ad_work_order
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER related_plan_id;

ALTER TABLE ad_work_order
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

ALTER TABLE ad_work_order
  ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除' AFTER close_time;

-- ad_feedback 缺少 create_by / update_by / deleted
ALTER TABLE ad_feedback
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER handler_id;

ALTER TABLE ad_feedback
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

ALTER TABLE ad_feedback
  ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除' AFTER update_time;

-- sys_menu 缺少 create_by / update_by / deleted
ALTER TABLE sys_menu
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER status;

ALTER TABLE sys_menu
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

ALTER TABLE sys_menu
  ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除' AFTER update_time;

-- sys_dict 缺少 create_by / update_by / update_time / deleted
ALTER TABLE sys_dict
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER remark;

ALTER TABLE sys_dict
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

ALTER TABLE sys_dict
  ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER update_by;

ALTER TABLE sys_dict
  ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除' AFTER update_time;

-- sys_dict_item 缺少 create_by / create_time / update_by / update_time / deleted
ALTER TABLE sys_dict_item
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER remark;

ALTER TABLE sys_dict_item
  ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER create_by;

ALTER TABLE sys_dict_item
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

ALTER TABLE sys_dict_item
  ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER update_by;

ALTER TABLE sys_dict_item
  ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除' AFTER update_time;

