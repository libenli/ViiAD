-- 百万广告系统 V1 数据库建表 SQL 初稿
-- 兼容目标：Java 8 + Spring Boot 2.6.x + MySQL 5.x
-- 说明：字段和索引用于首版开发评审，正式执行前请结合现有库的字符集、表前缀、账号权限再确认。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 系统基础表
-- ----------------------------

CREATE TABLE IF NOT EXISTS sys_dept (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  parent_id BIGINT DEFAULT 0 COMMENT '上级部门ID',
  dept_name VARCHAR(80) NOT NULL COMMENT '部门名称',
  dept_type VARCHAR(30) DEFAULT 'department' COMMENT '部门类型：company/region/department',
  region_code VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  leader VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  phone VARCHAR(30) DEFAULT NULL COMMENT '联系电话',
  sort_no INT DEFAULT 0 COMMENT '排序',
  status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  KEY idx_sys_dept_parent (parent_id),
  KEY idx_sys_dept_region (region_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门区域表';

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(50) NOT NULL COMMENT '登录账号',
  password VARCHAR(120) NOT NULL COMMENT '登录密码',
  nick_name VARCHAR(80) DEFAULT NULL COMMENT '昵称',
  real_name VARCHAR(80) DEFAULT NULL COMMENT '真实姓名',
  phone VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  dept_id BIGINT DEFAULT NULL COMMENT '部门ID',
  status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  is_admin TINYINT DEFAULT 0 COMMENT '是否管理员：1是 0否',
  last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username),
  KEY idx_sys_user_dept (dept_id),
  KEY idx_sys_user_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_name VARCHAR(80) NOT NULL COMMENT '角色名称',
  role_code VARCHAR(80) NOT NULL COMMENT '角色编码',
  data_scope VARCHAR(30) DEFAULT 'self' COMMENT '数据范围',
  status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (role_code),
  KEY idx_sys_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  parent_id BIGINT DEFAULT 0 COMMENT '上级菜单ID',
  menu_name VARCHAR(80) NOT NULL COMMENT '菜单名称',
  menu_type VARCHAR(20) NOT NULL COMMENT '菜单类型：catalog/menu/button',
  path VARCHAR(200) DEFAULT NULL COMMENT '路由地址',
  component VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
  permission VARCHAR(120) DEFAULT NULL COMMENT '权限标识',
  icon VARCHAR(80) DEFAULT NULL COMMENT '图标',
  sort_no INT DEFAULT 0 COMMENT '排序',
  visible TINYINT DEFAULT 1 COMMENT '是否显示：1显示 0隐藏',
  status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  KEY idx_sys_menu_parent (parent_id),
  KEY idx_sys_menu_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

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

CREATE TABLE IF NOT EXISTS sys_dict (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
  dict_code VARCHAR(100) NOT NULL COMMENT '字典编码',
  status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典表';

CREATE TABLE IF NOT EXISTS sys_dict_item (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '字典项ID',
  dict_code VARCHAR(100) NOT NULL COMMENT '字典编码',
  item_label VARCHAR(100) NOT NULL COMMENT '字典项名称',
  item_value VARCHAR(100) NOT NULL COMMENT '字典项值',
  sort_no INT DEFAULT 0 COMMENT '排序',
  status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  KEY idx_sys_dict_item_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';

-- ----------------------------
-- 广告主与代理商
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
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  create_by BIGINT DEFAULT NULL COMMENT '创建人',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by BIGINT DEFAULT NULL COMMENT '更新人',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记：0正常 1删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_ad_advertiser_code (advertiser_code),
  KEY idx_ad_advertiser_owner (owner_user_id),
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

-- ----------------------------
-- 广告、素材、计划
-- ----------------------------

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
  KEY idx_ad_order_region (region_code),
  KEY idx_ad_order_create_time (create_time)
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
  schedule_status VARCHAR(30) DEFAULT 'pending' COMMENT '排期状态',
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
  KEY idx_ad_plan_status (schedule_status, delivery_status),
  KEY idx_ad_plan_time (start_time, end_time)
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
-- 设备与楼宇
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

-- ----------------------------
-- 执行、报表、财务、服务
-- ----------------------------

CREATE TABLE IF NOT EXISTS ad_delivery_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '下发记录ID',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  delivery_type VARCHAR(30) DEFAULT 'manual' COMMENT '下发类型',
  delivery_status VARCHAR(30) DEFAULT 'pending' COMMENT '下发状态',
  response_msg VARCHAR(1000) DEFAULT NULL COMMENT '响应信息',
  retry_count INT DEFAULT 0 COMMENT '重试次数',
  delivery_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '下发时间',
  PRIMARY KEY (id),
  KEY idx_ad_delivery_plan (plan_id),
  KEY idx_ad_delivery_device (device_id),
  KEY idx_ad_delivery_status (delivery_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计划下发记录表';

CREATE TABLE IF NOT EXISTS ad_play_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '播放日志ID',
  ad_id BIGINT NOT NULL COMMENT '广告ID',
  plan_id BIGINT NOT NULL COMMENT '计划ID',
  material_id BIGINT DEFAULT NULL COMMENT '素材ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  play_date DATE NOT NULL COMMENT '播放日期',
  play_count INT DEFAULT 0 COMMENT '播放次数',
  play_duration INT DEFAULT 0 COMMENT '播放时长秒',
  source_type VARCHAR(30) DEFAULT 'device' COMMENT '数据来源',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_ad_play_log_date (play_date),
  KEY idx_ad_play_log_ad (ad_id),
  KEY idx_ad_play_log_plan (plan_id),
  KEY idx_ad_play_log_device (device_id)
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
-- 日志
-- ----------------------------

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

CREATE TABLE IF NOT EXISTS sys_login_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '登录日志ID',
  username VARCHAR(80) DEFAULT NULL COMMENT '用户名',
  login_status TINYINT DEFAULT 1 COMMENT '登录状态：1成功 0失败',
  login_ip VARCHAR(80) DEFAULT NULL COMMENT '登录IP',
  browser VARCHAR(120) DEFAULT NULL COMMENT '浏览器',
  os VARCHAR(120) DEFAULT NULL COMMENT '操作系统',
  message VARCHAR(500) DEFAULT NULL COMMENT '提示信息',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_sys_login_log_time (create_time),
  KEY idx_sys_login_log_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

SET FOREIGN_KEY_CHECKS = 1;
