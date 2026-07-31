-- 操作日志页面权限补丁；状态字典由后端接口 /api/system/dicts 提供，无需额外建表。

CREATE TABLE IF NOT EXISTS sys_oper_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  module_name VARCHAR(80) DEFAULT NULL COMMENT '模块名称',
  business_type VARCHAR(50) DEFAULT NULL COMMENT '业务类型',
  request_uri VARCHAR(300) DEFAULT NULL COMMENT '请求地址',
  request_method VARCHAR(20) DEFAULT NULL COMMENT '请求方法',
  operator_name VARCHAR(80) DEFAULT NULL COMMENT '操作人',
  operator_ip VARCHAR(80) DEFAULT NULL COMMENT '操作IP',
  request_param TEXT COMMENT '请求参数',
  response_result TEXT COMMENT '响应结果',
  cost_time BIGINT DEFAULT 0 COMMENT '耗时毫秒',
  status TINYINT DEFAULT 1 COMMENT '状态：1成功 0失败',
  error_msg TEXT COMMENT '错误信息',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_sys_oper_log_time (create_time),
  KEY idx_sys_oper_log_operator (operator_name),
  KEY idx_sys_oper_log_module (module_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

INSERT IGNORE INTO sys_menu (id, menu_name, menu_path, permission_code, icon, sort_no) VALUES
(14, '操作日志', '/system/oper-logs', 'system:log:view', 'Document', 140);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id = 14;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2, 14),
(12, 14);
