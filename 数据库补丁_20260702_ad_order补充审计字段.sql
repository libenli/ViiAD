-- 修复广告保存时报错：Unknown column 'create_by' in 'field list'
-- 原因：后台 AdOrder 继承 BaseEntity，MyBatis-Plus 会映射 create_by / update_by 字段。
-- 使用方式：在当前业务数据库执行一次即可。

ALTER TABLE ad_order
  ADD COLUMN create_by BIGINT DEFAULT NULL COMMENT '创建人' AFTER create_user_id;

ALTER TABLE ad_order
  ADD COLUMN update_by BIGINT DEFAULT NULL COMMENT '更新人' AFTER create_time;

