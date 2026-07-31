-- 登录安全加固：登录日志表
-- 说明：旧 sys_user.password 可继续保留明文，用户首次成功登录后会自动升级为 BCrypt。

CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  username VARCHAR(80) NOT NULL COMMENT '登录账号',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  login_status VARCHAR(20) NOT NULL COMMENT '登录结果：success/fail',
  fail_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  ip_address VARCHAR(80) DEFAULT NULL COMMENT '登录IP',
  user_agent VARCHAR(500) DEFAULT NULL COMMENT '浏览器标识',
  login_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (id),
  KEY idx_sys_login_log_username (username),
  KEY idx_sys_login_log_status (login_status),
  KEY idx_sys_login_log_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
