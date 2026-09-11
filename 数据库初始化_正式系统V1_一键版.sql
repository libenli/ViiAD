-- 百万广告系统 正式系统 V1 一键初始化脚本
-- 适用：空数据库首次部署。兼容 MySQL 5.x。
-- 特点：建表 + RBAC 菜单权限 + 13 类演示账号 + 基础业务演示数据。
-- 注意：本脚本尽量可重复执行，使用 CREATE TABLE IF NOT EXISTS / INSERT IGNORE。
--       如果已经执行过早期“建表初稿”并产生不兼容表结构，请先备份后按说明处理，不建议直接混跑。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 系统与权限
-- ----------------------------

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(80) NOT NULL COMMENT '登录账号',
  password VARCHAR(120) NOT NULL COMMENT '登录密码',
  real_name VARCHAR(80) NOT NULL COMMENT '姓名',
  phone VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  user_type VARCHAR(30) DEFAULT 'platform' COMMENT '用户类型：platform/advertiser/agent',
  advertiser_id BIGINT DEFAULT NULL COMMENT '绑定广告主ID',
  agent_id BIGINT DEFAULT NULL COMMENT '绑定代理商ID',
  status VARCHAR(30) DEFAULT 'active' COMMENT '状态：active/disabled',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(80) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(120) NOT NULL COMMENT '角色名称',
  status VARCHAR(30) DEFAULT 'active' COMMENT '状态：active/disabled',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  menu_name VARCHAR(120) NOT NULL COMMENT '菜单名称',
  menu_path VARCHAR(200) NOT NULL COMMENT '前端路径',
  permission_code VARCHAR(120) DEFAULT NULL COMMENT '权限编码',
  icon VARCHAR(80) DEFAULT NULL COMMENT '图标',
  sort_no INT DEFAULT 0 COMMENT '排序',
  status VARCHAR(30) DEFAULT 'active' COMMENT '状态',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_menu_path (menu_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

CREATE TABLE IF NOT EXISTS sys_user_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role (user_id, role_id),
  KEY idx_sys_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  menu_id BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_menu (role_id, menu_id),
  KEY idx_sys_role_menu_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_role_data_scope (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  role_id BIGINT NOT NULL COMMENT '角色ID',
  scope_type VARCHAR(30) DEFAULT 'all' COMMENT 'all/platform/advertiser/agent',
  advertiser_id BIGINT DEFAULT NULL COMMENT '广告主ID',
  agent_id BIGINT DEFAULT NULL COMMENT '代理商ID',
  region_code VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  PRIMARY KEY (id),
  KEY idx_sys_role_scope_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据权限表';

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

-- ----------------------------
-- 业务主体
-- ----------------------------

CREATE TABLE IF NOT EXISTS ad_advertiser (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '广告主ID',
  advertiser_code VARCHAR(50) NOT NULL COMMENT '广告主编号',
  advertiser_name VARCHAR(120) NOT NULL COMMENT '广告主名称',
  company_name VARCHAR(160) DEFAULT NULL COMMENT '公司名称',
  contact_name VARCHAR(80) DEFAULT NULL COMMENT '联系人',
  contact_phone VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
  contact_email VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  owner_user_id BIGINT DEFAULT NULL COMMENT '绑定用户ID',
  status VARCHAR(30) DEFAULT 'active' COMMENT '状态',
  source_type VARCHAR(30) DEFAULT 'platform' COMMENT '来源',
  agent_id BIGINT DEFAULT NULL COMMENT '来源代理商ID',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_advertiser_code (advertiser_code),
  KEY idx_ad_advertiser_owner (owner_user_id),
  KEY idx_ad_advertiser_agent (agent_id),
  KEY idx_ad_advertiser_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告主表';

CREATE TABLE IF NOT EXISTS ad_agent (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '代理商ID',
  agent_code VARCHAR(50) NOT NULL COMMENT '代理商编号',
  agent_name VARCHAR(120) NOT NULL COMMENT '代理商名称',
  contact_name VARCHAR(80) DEFAULT NULL COMMENT '联系人',
  contact_phone VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
  contact_email VARCHAR(100) DEFAULT NULL COMMENT '联系邮箱',
  owner_user_id BIGINT DEFAULT NULL COMMENT '绑定用户ID',
  status VARCHAR(30) DEFAULT 'active' COMMENT '状态',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_agent_code (agent_code),
  KEY idx_ad_agent_owner (owner_user_id),
  KEY idx_ad_agent_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告代理商表';

CREATE TABLE IF NOT EXISTS ad_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '广告ID',
  ad_code VARCHAR(50) NOT NULL COMMENT '广告编号',
  ad_name VARCHAR(160) NOT NULL COMMENT '广告名称',
  advertiser_id BIGINT NOT NULL COMMENT '广告主ID',
  agent_id BIGINT DEFAULT NULL COMMENT '代理商ID',
  ad_type VARCHAR(30) NOT NULL COMMENT '广告类型：image/video/interactive',
  objective VARCHAR(30) DEFAULT NULL COMMENT '投放目标',
  region_code VARCHAR(50) DEFAULT NULL COMMENT '投放区域',
  budget_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '预算金额',
  description VARCHAR(1000) DEFAULT NULL COMMENT '广告说明',
  status VARCHAR(30) DEFAULT 'draft' COMMENT '状态',
  submit_time DATETIME DEFAULT NULL COMMENT '提交时间',
  audit_user_id BIGINT DEFAULT NULL COMMENT '审核人',
  audit_comment VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
  create_user_id BIGINT DEFAULT NULL COMMENT '创建用户',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_order_code (ad_code),
  KEY idx_ad_order_advertiser (advertiser_id),
  KEY idx_ad_order_agent (agent_id),
  KEY idx_ad_order_status (status),
  KEY idx_ad_order_region (region_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告单表';

CREATE TABLE IF NOT EXISTS ad_material (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '素材ID',
  material_code VARCHAR(50) NOT NULL COMMENT '素材编号',
  ad_id BIGINT NOT NULL COMMENT '广告ID',
  material_name VARCHAR(160) NOT NULL COMMENT '素材名称',
  material_type VARCHAR(30) NOT NULL COMMENT '素材类型：image/video/h5',
  file_url VARCHAR(500) NOT NULL COMMENT '文件地址',
  file_size BIGINT DEFAULT 0 COMMENT '文件大小',
  duration_seconds INT DEFAULT NULL COMMENT '视频时长秒',
  width INT DEFAULT NULL COMMENT '宽度',
  height INT DEFAULT NULL COMMENT '高度',
  cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面地址',
  status VARCHAR(30) DEFAULT 'draft' COMMENT '状态',
  review_user_id BIGINT DEFAULT NULL COMMENT '审核人',
  review_comment VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  review_time DATETIME DEFAULT NULL COMMENT '审核时间',
  uploader_id BIGINT DEFAULT NULL COMMENT '上传人',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_material_code (material_code),
  KEY idx_ad_material_ad (ad_id),
  KEY idx_ad_material_status (status),
  KEY idx_ad_material_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告素材表';

CREATE TABLE IF NOT EXISTS ad_plan (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '计划ID',
  plan_code VARCHAR(50) NOT NULL COMMENT '计划编号',
  plan_name VARCHAR(160) NOT NULL COMMENT '计划名称',
  ad_id BIGINT NOT NULL COMMENT '广告ID',
  region_code VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  schedule_status VARCHAR(30) DEFAULT 'draft' COMMENT '排期状态',
  delivery_status VARCHAR(30) DEFAULT 'not_started' COMMENT '投放状态',
  operator_id BIGINT DEFAULT NULL COMMENT '运营负责人',
  create_user_id BIGINT DEFAULT NULL COMMENT '创建人',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_plan_code (plan_code),
  KEY idx_ad_plan_ad (ad_id),
  KEY idx_ad_plan_region (region_code),
  KEY idx_ad_plan_status (schedule_status, delivery_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投放计划表';

CREATE TABLE IF NOT EXISTS ad_plan_material (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  material_id BIGINT NOT NULL COMMENT '素材ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_plan_material (plan_id, material_id),
  KEY idx_ad_plan_material_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划素材关联表';

CREATE TABLE IF NOT EXISTS ad_plan_device (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_plan_device (plan_id, device_id),
  KEY idx_ad_plan_device_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划设备关联表';

CREATE TABLE IF NOT EXISTS ad_plan_time_slot (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '时段ID',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  week_day TINYINT NOT NULL COMMENT '星期：1-7',
  start_clock VARCHAR(10) NOT NULL COMMENT '开始时刻',
  end_clock VARCHAR(10) NOT NULL COMMENT '结束时刻',
  PRIMARY KEY (id),
  KEY idx_ad_plan_time_slot_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划投放时段表';

CREATE TABLE IF NOT EXISTS ad_plan_audience_tag (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  tag_name VARCHAR(80) NOT NULL COMMENT '人群标签',
  PRIMARY KEY (id),
  KEY idx_ad_plan_audience_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划人群标签表';

-- ----------------------------
-- 设备与投放
-- ----------------------------

CREATE TABLE IF NOT EXISTS ad_building (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '楼宇ID',
  building_code VARCHAR(50) NOT NULL COMMENT '楼宇编号',
  building_name VARCHAR(160) NOT NULL COMMENT '楼宇名称',
  region_code VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  address VARCHAR(300) DEFAULT NULL COMMENT '地址',
  longitude DECIMAL(12,6) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(12,6) DEFAULT NULL COMMENT '纬度',
  status VARCHAR(30) DEFAULT 'active' COMMENT '状态',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_building_code (building_code),
  KEY idx_ad_building_region (region_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼宇表';

CREATE TABLE IF NOT EXISTS ad_device (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  device_code VARCHAR(50) NOT NULL COMMENT '设备编号',
  device_name VARCHAR(160) NOT NULL COMMENT '设备名称',
  building_id BIGINT DEFAULT NULL COMMENT '楼宇ID',
  floor_no VARCHAR(30) DEFAULT NULL COMMENT '楼层',
  screen_size VARCHAR(50) DEFAULT NULL COMMENT '屏幕尺寸',
  resolution VARCHAR(50) DEFAULT NULL COMMENT '分辨率',
  ip_address VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  mac_address VARCHAR(80) DEFAULT NULL COMMENT 'MAC地址',
  online_status VARCHAR(30) DEFAULT 'offline' COMMENT '在线状态',
  fault_status VARCHAR(30) DEFAULT 'normal' COMMENT '故障状态',
  last_online_time DATETIME DEFAULT NULL COMMENT '最后在线时间',
  current_plan_id BIGINT DEFAULT NULL COMMENT '当前计划ID',
  status VARCHAR(30) DEFAULT 'active' COMMENT '设备状态',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_device_code (device_code),
  KEY idx_ad_device_building (building_id),
  KEY idx_ad_device_online (online_status),
  KEY idx_ad_device_current_plan (current_plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

CREATE TABLE IF NOT EXISTS ad_device_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  log_type VARCHAR(50) NOT NULL COMMENT '日志类型',
  content VARCHAR(1000) DEFAULT NULL COMMENT '日志内容',
  log_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '日志时间',
  PRIMARY KEY (id),
  KEY idx_ad_device_log_device (device_id),
  KEY idx_ad_device_log_time (log_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备日志表';

CREATE TABLE IF NOT EXISTS ad_delivery_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '下发记录ID',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  delivery_type VARCHAR(30) DEFAULT 'manual' COMMENT '下发类型',
  delivery_status VARCHAR(30) DEFAULT 'pending' COMMENT '下发状态',
  response_msg VARCHAR(1000) DEFAULT NULL COMMENT '响应信息',
  request_id VARCHAR(64) DEFAULT NULL COMMENT '设备指令请求ID',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  delivery_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
  ack_time DATETIME DEFAULT NULL COMMENT '设备ACK时间',
  ack_message VARCHAR(1000) DEFAULT NULL COMMENT '设备ACK消息',
  PRIMARY KEY (id),
  KEY idx_ad_delivery_plan (plan_id),
  KEY idx_ad_delivery_device (device_id),
  KEY idx_ad_delivery_status (delivery_status),
  UNIQUE KEY uk_ad_delivery_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划下发记录表';

-- ----------------------------
-- 报表、账单、工单
-- ----------------------------

CREATE TABLE IF NOT EXISTS ad_play_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '播放日志ID',
  ad_id BIGINT NOT NULL COMMENT '广告ID',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  material_id BIGINT DEFAULT NULL COMMENT '素材ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  play_date DATE NOT NULL COMMENT '播放日期',
  play_count INT DEFAULT 0 COMMENT '播放次数',
  play_duration INT DEFAULT 0 COMMENT '播放时长秒',
  play_status VARCHAR(30) DEFAULT 'completed' COMMENT '播放状态：playing/completed/failed/paused',
  play_start_time DATETIME DEFAULT NULL COMMENT '播放开始时间',
  play_end_time DATETIME DEFAULT NULL COMMENT '播放结束时间',
  request_id VARCHAR(80) DEFAULT NULL COMMENT '关联下发请求ID',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '播放异常信息',
  source_type VARCHAR(30) DEFAULT 'device' COMMENT '数据来源',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ad_play_log_date (play_date),
  KEY idx_ad_play_log_ad (ad_id),
  KEY idx_ad_play_log_plan (plan_id),
  KEY idx_ad_play_log_device (device_id),
  KEY idx_ad_play_log_status (play_status),
  KEY idx_ad_play_log_request (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='播放日志表';

CREATE TABLE IF NOT EXISTS ad_report_daily (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日报ID',
  stat_date DATE NOT NULL COMMENT '统计日期',
  ad_id BIGINT DEFAULT NULL COMMENT '广告ID',
  plan_id BIGINT DEFAULT NULL COMMENT '计划ID',
  advertiser_id BIGINT DEFAULT NULL COMMENT '广告主ID',
  agent_id BIGINT DEFAULT NULL COMMENT '代理商ID',
  region_code VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  play_count INT DEFAULT 0 COMMENT '播放次数',
  exposure_count INT DEFAULT 0 COMMENT '曝光次数',
  online_device_count INT DEFAULT 0 COMMENT '在线设备数',
  abnormal_device_count INT DEFAULT 0 COMMENT '异常设备数',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ad_report_daily_date (stat_date),
  KEY idx_ad_report_daily_ad (ad_id),
  KEY idx_ad_report_daily_region (region_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告投放日报表';

CREATE TABLE IF NOT EXISTS ad_bill (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '账单ID',
  bill_no VARCHAR(50) NOT NULL COMMENT '账单编号',
  bill_type VARCHAR(30) NOT NULL COMMENT '账单类型：advertiser/agent',
  advertiser_id BIGINT DEFAULT NULL COMMENT '广告主ID',
  agent_id BIGINT DEFAULT NULL COMMENT '代理商ID',
  bill_month VARCHAR(20) NOT NULL COMMENT '账单月份',
  amount_total DECIMAL(12,2) DEFAULT 0.00 COMMENT '应收金额',
  amount_paid DECIMAL(12,2) DEFAULT 0.00 COMMENT '已收金额',
  status VARCHAR(30) DEFAULT 'pending' COMMENT '状态',
  confirm_time DATETIME DEFAULT NULL COMMENT '确认时间',
  pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
  payment_voucher_no VARCHAR(100) DEFAULT NULL COMMENT '银行汇款单号',
  payment_voucher_url VARCHAR(500) DEFAULT NULL COMMENT '付款截图地址',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_bill_no (bill_no),
  KEY idx_ad_bill_month (bill_month),
  KEY idx_ad_bill_status (status),
  KEY idx_ad_bill_owner (advertiser_id, agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单表';

CREATE TABLE IF NOT EXISTS ad_bill_detail (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '账单明细ID',
  bill_id BIGINT NOT NULL COMMENT '账单ID',
  ad_id BIGINT DEFAULT NULL COMMENT '广告ID',
  plan_id BIGINT DEFAULT NULL COMMENT '计划ID',
  item_name VARCHAR(160) NOT NULL COMMENT '明细名称',
  item_amount DECIMAL(12,2) DEFAULT 0.00 COMMENT '明细金额',
  item_count INT DEFAULT 0 COMMENT '计费数量',
  PRIMARY KEY (id),
  KEY idx_ad_bill_detail_bill (bill_id),
  KEY idx_ad_bill_detail_ad (ad_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细表';

CREATE TABLE IF NOT EXISTS ad_work_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  work_no VARCHAR(50) NOT NULL COMMENT '工单编号',
  source_type VARCHAR(30) DEFAULT 'manual' COMMENT '来源类型',
  title VARCHAR(160) NOT NULL COMMENT '标题',
  content VARCHAR(2000) DEFAULT NULL COMMENT '内容',
  priority VARCHAR(30) DEFAULT 'normal' COMMENT '优先级',
  status VARCHAR(30) DEFAULT 'open' COMMENT '状态',
  creator_id BIGINT DEFAULT NULL COMMENT '创建人',
  assignee_id BIGINT DEFAULT NULL COMMENT '处理人',
  related_ad_id BIGINT DEFAULT NULL COMMENT '关联广告ID',
  related_plan_id BIGINT DEFAULT NULL COMMENT '关联计划ID',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  close_time DATETIME DEFAULT NULL COMMENT '关闭时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_work_order_no (work_no),
  KEY idx_ad_work_order_status (status),
  KEY idx_ad_work_order_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

CREATE TABLE IF NOT EXISTS ad_feedback (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  feedback_type VARCHAR(50) NOT NULL COMMENT '反馈类型',
  contact_name VARCHAR(80) DEFAULT NULL COMMENT '联系人',
  contact_phone VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
  content VARCHAR(2000) NOT NULL COMMENT '反馈内容',
  status VARCHAR(30) DEFAULT 'pending' COMMENT '状态',
  handler_id BIGINT DEFAULT NULL COMMENT '处理人',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  KEY idx_ad_feedback_status (status),
  KEY idx_ad_feedback_handler (handler_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈表';

-- ----------------------------
-- RBAC：13 类角色、账号、菜单和权限
-- ----------------------------

INSERT IGNORE INTO sys_role (id, role_code, role_name, remark) VALUES
(1, 'super_admin', '超级管理员', '拥有全部菜单和权限'),
(2, 'developer', '开发工程师', '负责系统联调和技术排障'),
(3, 'ops_engineer', '运维工程师', '负责设备、下发和运行状态'),
(4, 'region_admin', '区域管理员', '负责区域设备和区域数据'),
(5, 'business_operator', '业务运营', '负责广告、素材、计划、设备和下发'),
(6, 'material_auditor', '广告素材审核', '负责广告和素材审核'),
(7, 'finance', '财务账号', '负责报表和账单结算'),
(8, 'data_analyst', '数据分析师', '负责数据报表查看'),
(9, 'agent_user', '广告代理商账号', '代理商侧查看广告、报表和账单'),
(10, 'advertiser_user', '广告主账号', '广告主侧查看广告、报表和账单'),
(11, 'customer_service', '客服账号', '负责工单反馈处理'),
(12, 'auditor', '审计账号', '负责全流程查看和审计'),
(13, 'audience_user', '受众账号', '预留受众侧查看角色');

INSERT IGNORE INTO sys_user (id, username, password, real_name, user_type, advertiser_id, agent_id, status) VALUES
(1, 'admin@mysher.com', 'admin123', '系统管理员', 'platform', NULL, NULL, 'active'),
(2, 'developer@mysher.com', 'admin123', '开发工程师', 'platform', NULL, NULL, 'active'),
(3, 'operator@mysher.com', 'admin123', '运维工程师', 'platform', NULL, NULL, 'active'),
(4, 'region@mysher.com', 'admin123', '区域管理员', 'platform', NULL, NULL, 'active'),
(5, 'business@mysher.com', 'admin123', '业务运营', 'platform', NULL, NULL, 'active'),
(6, 'reviewer@mysher.com', 'admin123', '素材审核员', 'platform', NULL, NULL, 'active'),
(7, 'finance@mysher.com', 'admin123', '财务人员', 'platform', NULL, NULL, 'active'),
(8, 'analyst@mysher.com', 'admin123', '数据分析师', 'platform', NULL, NULL, 'active'),
(9, 'agent01@mysher.com', 'admin123', '代理商账号', 'agent', NULL, 1, 'active'),
(10, 'advertiser01@mysher.com', 'admin123', '广告主账号', 'advertiser', 1, NULL, 'active'),
(11, 'service@mysher.com', 'admin123', '客服人员', 'platform', NULL, NULL, 'active'),
(12, 'auditor@mysher.com', 'admin123', '审计人员', 'platform', NULL, NULL, 'active'),
(13, 'audience@mysher.com', 'admin123', '受众账号', 'platform', NULL, NULL, 'active');

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (12, 12), (13, 13);

INSERT IGNORE INTO sys_menu (id, menu_name, menu_path, permission_code, icon, sort_no) VALUES
(1, '工作台', '/dashboard', 'dashboard:view', 'DataBoard', 10),
(2, '广告列表', '/ads', 'ad:view', 'Promotion', 70),
(3, '素材管理', '/materials', 'material:view', 'Picture', 80),
(4, '投放计划', '/plans', 'plan:view', 'Calendar', 90),
(5, '设备管理', '/devices', 'device:view', 'Monitor', 40),
(6, '下发记录', '/deliveries', 'delivery:view', 'Connection', 100),
(7, '数据报表', '/reports/overview', 'report:view', 'TrendCharts', 120),
(8, '账单结算', '/finance/bills', 'bill:view', 'Wallet', 130),
(9, '工单反馈', '/workorders', 'workOrder:view', 'Service', 110),
(10, '广告主', '/partners/advertisers', 'advertiser:view', 'OfficeBuilding', 60),
(11, '代理商', '/partners/agents', 'agent:view', 'UserFilled', 50),
(12, '用户管理', '/system/users', 'system:user:view', 'User', 20),
(13, '权限管理', '/system/roles', 'system:role:view', 'Setting', 30),
(14, '操作日志', '/system/oper-logs', 'system:log:view', 'Document', 140),
(101, '广告编辑', '#ad_edit', 'ad:edit', NULL, 201),
(102, '广告提交', '#ad_submit', 'ad:submit', NULL, 202),
(103, '广告审核', '#ad_audit', 'ad:audit', NULL, 203),
(104, '素材编辑', '#material_edit', 'material:edit', NULL, 211),
(105, '素材提交', '#material_submit', 'material:submit', NULL, 212),
(106, '素材审核', '#material_audit', 'material:audit', NULL, 213),
(107, '计划编辑', '#plan_edit', 'plan:edit', NULL, 221),
(108, '计划排期', '#plan_schedule', 'plan:schedule', NULL, 222),
(109, '计划投放', '#plan_delivery', 'plan:delivery', NULL, 223),
(110, '设备管理操作', '#device_manage', 'device:manage', NULL, 231),
(111, '下发操作', '#delivery_operate', 'delivery:operate', NULL, 241),
(112, '账单确认', '#bill_confirm', 'bill:confirm', NULL, 251),
(113, '账单支付', '#bill_pay', 'bill:pay', NULL, 252),
(114, '工单操作', '#workorder_operate', 'workOrder:operate', NULL, 261),
(115, '客户主体管理', '#partner_manage', 'partner:manage', NULL, 271),
(116, '角色授权', '#system_role_grant', 'system:role:grant', NULL, 281),
(117, '用户管理操作', '#system_user_manage', 'system:user:manage', NULL, 282);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 5), (2, 6), (2, 7), (2, 12), (2, 13), (2, 14), (2, 110), (2, 111), (2, 116), (2, 117),
(3, 1), (3, 5), (3, 6), (3, 9), (3, 110), (3, 111), (3, 114),
(4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 9),
(5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6), (5, 10), (5, 11), (5, 101), (5, 102), (5, 104), (5, 105), (5, 107), (5, 108), (5, 109), (5, 110), (5, 111), (5, 115),
(6, 1), (6, 2), (6, 3), (6, 103), (6, 106),
(7, 1), (7, 7), (7, 8), (7, 112), (7, 113),
(8, 1), (8, 7),
(9, 1), (9, 2), (9, 3), (9, 4), (9, 7), (9, 8), (9, 9),
(10, 1), (10, 2), (10, 3), (10, 4), (10, 7), (10, 8), (10, 9),
(11, 1), (11, 5), (11, 6), (11, 9), (11, 114),
(12, 1), (12, 2), (12, 3), (12, 4), (12, 5), (12, 6), (12, 7), (12, 8), (12, 9), (12, 10), (12, 11), (12, 12), (12, 13), (12, 14),
(13, 1), (13, 7);

INSERT IGNORE INTO sys_role_data_scope (id, role_id, scope_type, advertiser_id, agent_id) VALUES
(1, 1, 'all', NULL, NULL),
(2, 2, 'platform', NULL, NULL),
(3, 3, 'platform', NULL, NULL),
(4, 4, 'platform', NULL, NULL),
(5, 5, 'platform', NULL, NULL),
(6, 6, 'platform', NULL, NULL),
(7, 7, 'platform', NULL, NULL),
(8, 8, 'platform', NULL, NULL),
(9, 9, 'agent', NULL, 1),
(10, 10, 'advertiser', 1, NULL),
(11, 11, 'platform', NULL, NULL),
(12, 12, 'platform', NULL, NULL),
(13, 13, 'platform', NULL, NULL);

-- ----------------------------
-- 演示业务数据
-- ----------------------------

INSERT IGNORE INTO ad_agent (id, agent_code, agent_name, contact_name, contact_phone, contact_email, owner_user_id, status, remark)
VALUES
(1, 'AGT-DEMO-001', '星河传媒代理商', '赵经理', '13800000009', 'agent01@example.com', 9, 'active', '演示代理商');

INSERT IGNORE INTO ad_advertiser (id, advertiser_code, advertiser_name, company_name, contact_name, contact_phone, contact_email, owner_user_id, status, source_type, agent_id, remark)
VALUES
(1, 'ADV-DEMO-001', '蓝海汽车广告主', '蓝海汽车有限公司', '李经理', '13800000010', 'advertiser01@example.com', 10, 'active', 'platform', NULL, '演示广告主'),
(2, 'ADV-DEMO-002', '城市生活广告主', '城市生活服务有限公司', '王经理', '13800000011', 'city@example.com', NULL, 'active', 'platform', NULL, '演示广告主');

INSERT IGNORE INTO ad_building (id, building_code, building_name, region_code, address, longitude, latitude, status)
VALUES
(1, 'BLD-SH-001', '上海中庭中心', '上海', '上海市浦东新区世纪大道 100 号', 121.506000, 31.245000, 'active'),
(2, 'BLD-BJ-001', '北京金融大厦', '北京', '北京市朝阳区建国路 88 号', 116.475000, 39.914000, 'active');

INSERT IGNORE INTO ad_device (id, device_code, device_name, building_id, floor_no, screen_size, resolution, ip_address, mac_address, online_status, fault_status, last_online_time, current_plan_id, status)
VALUES
(1, 'DEV-SH-001', '上海中庭一层大屏', 1, '1F', '98寸', '3840x2160', '192.168.10.11', '00:11:22:33:44:01', 'online', 'normal', NOW(), 1, 'active'),
(2, 'DEV-SH-002', '上海中庭三层电梯屏', 1, '3F', '55寸', '1920x1080', '192.168.10.12', '00:11:22:33:44:02', 'online', 'normal', NOW(), 1, 'active'),
(3, 'DEV-BJ-001', '北京金融大厦大厅屏', 2, '1F', '86寸', '3840x2160', '192.168.20.11', '00:11:22:33:44:03', 'offline', 'normal', NULL, NULL, 'active'),
(4, 'DEV-BJ-002', '北京金融大厦会议层屏', 2, '18F', '65寸', '1920x1080', '192.168.20.12', '00:11:22:33:44:04', 'offline', 'fault', NULL, NULL, 'active');

INSERT IGNORE INTO ad_order (id, ad_code, ad_name, advertiser_id, agent_id, ad_type, objective, region_code, budget_amount, description, status, submit_time, audit_user_id, audit_comment, audit_time, create_user_id, create_by)
VALUES
(1, 'AD-DEMO-001', '蓝海汽车新能源上市广告', 1, 1, 'image', 'exposure', '上海', 120000.00, '演示主流程：已审核广告，可直接创建计划。', 'approved', NOW(), 6, '演示数据审核通过', NOW(), 10, 10),
(2, 'AD-DEMO-002', '城市生活会员活动广告', 2, NULL, 'video', 'conversion', '北京', 68000.00, '演示数据：待提交广告。', 'draft', NULL, NULL, NULL, NULL, 5, 5);

INSERT IGNORE INTO ad_material (id, material_code, ad_id, material_name, material_type, file_url, file_size, duration_seconds, width, height, cover_url, status, review_user_id, review_comment, review_time, uploader_id, create_by)
VALUES
(1, 'MAT-DEMO-001', 1, '蓝海汽车主视觉横版图', 'image', 'https://example.com/demo/blue-car-main.png', 2048000, NULL, 1920, 1080, NULL, 'approved', 6, '演示素材审核通过', NOW(), 10, 10),
(2, 'MAT-DEMO-002', 1, '蓝海汽车 15 秒视频', 'video', 'https://example.com/demo/blue-car-15s.mp4', 12800000, 15, 1920, 1080, 'https://example.com/demo/blue-car-cover.png', 'approved', 6, '演示素材审核通过', NOW(), 10, 10),
(3, 'MAT-DEMO-003', 2, '城市生活活动海报', 'image', 'https://example.com/demo/city-life.png', 1800000, NULL, 1080, 1920, NULL, 'draft', NULL, NULL, NULL, 5, 5);

INSERT IGNORE INTO ad_plan (id, plan_code, plan_name, ad_id, region_code, start_time, end_time, schedule_status, delivery_status, operator_id, create_user_id, create_by)
VALUES
(1, 'PLAN-DEMO-001', '上海核心商圈新能源广告投放', 1, '上海', '2026-07-01 00:00:00', '2026-07-31 23:59:59', 'scheduled', 'live', 5, 5, 5),
(2, 'PLAN-DEMO-002', '北京金融楼宇试投计划', 1, '北京', '2026-07-05 00:00:00', '2026-07-20 23:59:59', 'draft', 'not_started', 5, 5, 5);

INSERT IGNORE INTO ad_plan_material (plan_id, material_id) VALUES
(1, 1), (1, 2), (2, 1);

INSERT IGNORE INTO ad_plan_device (plan_id, device_id) VALUES
(1, 1), (1, 2), (2, 3);

INSERT IGNORE INTO ad_delivery_record (id, plan_id, device_id, delivery_type, delivery_status, response_msg, retry_count, delivery_time)
VALUES
(1, 1, 1, 'auto', 'success', '设备已接收投放计划', 0, NOW()),
(2, 1, 2, 'auto', 'success', '设备已接收投放计划', 0, NOW()),
(3, 2, 3, 'manual', 'pending', '待下发', 0, NOW());

INSERT IGNORE INTO ad_play_log (id, ad_id, plan_id, material_id, device_id, play_date, play_count, play_duration, source_type)
VALUES
(1, 1, 1, 1, 1, CURDATE(), 128, 3840, 'device'),
(2, 1, 1, 2, 2, CURDATE(), 96, 1440, 'device'),
(3, 1, 1, 1, 1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 156, 4680, 'device');

INSERT IGNORE INTO ad_report_daily (id, stat_date, ad_id, plan_id, advertiser_id, agent_id, region_code, play_count, exposure_count, online_device_count, abnormal_device_count)
VALUES
(1, CURDATE(), 1, 1, 1, 1, '上海', 224, 4480, 2, 0),
(2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 1, 1, 1, 1, '上海', 156, 3120, 2, 0);

INSERT IGNORE INTO ad_bill (id, bill_no, bill_type, advertiser_id, agent_id, bill_month, amount_total, amount_paid, status, confirm_time, pay_time)
VALUES
(1, 'BILL-DEMO-202607-001', 'advertiser', 1, NULL, '2026-07', 12800.00, 0.00, 'pending', NULL, NULL),
(2, 'BILL-DEMO-202607-002', 'agent', NULL, 1, '2026-07', 3200.00, 3200.00, 'paid', NOW(), NOW());

INSERT IGNORE INTO ad_bill_detail (id, bill_id, ad_id, plan_id, item_name, item_amount, item_count)
VALUES
(1, 1, 1, 1, '上海核心商圈曝光计费', 12800.00, 4480),
(2, 2, 1, 1, '代理服务结算', 3200.00, 1);

INSERT IGNORE INTO ad_work_order (id, work_no, source_type, title, content, priority, status, creator_id, assignee_id, related_ad_id, related_plan_id, create_by)
VALUES
(1, 'WO-DEMO-001', 'device', '北京金融大厦会议层屏离线', '设备上报离线且疑似网络异常，请运维排查。', 'high', 'open', 3, 3, 1, 2, 3),
(2, 'WO-DEMO-002', 'manual', '广告主咨询投放日报', '广告主希望查看本周曝光趋势。', 'normal', 'assigned', 10, 11, 1, 1, 10);

INSERT IGNORE INTO ad_feedback (id, feedback_type, contact_name, contact_phone, content, status, handler_id, create_by)
VALUES
(1, 'consult', '李经理', '13800000010', '希望增加按楼宇维度查看播放数据。', 'pending', 11, 10),
(2, 'fault', '赵经理', '13800000009', '代理商反馈部分设备播放延迟。', 'processing', 11, 9);

SET FOREIGN_KEY_CHECKS = 1;
