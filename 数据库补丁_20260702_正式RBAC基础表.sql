CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(80) NOT NULL COMMENT '登录账号',
  password VARCHAR(120) NOT NULL COMMENT '登录密码',
  real_name VARCHAR(80) NOT NULL COMMENT '姓名',
  phone VARCHAR(30) DEFAULT NULL COMMENT '手机号',
  email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  user_type VARCHAR(30) DEFAULT 'platform' COMMENT '用户类型',
  advertiser_id BIGINT DEFAULT NULL COMMENT '绑定广告主ID',
  agent_id BIGINT DEFAULT NULL COMMENT '绑定代理商ID',
  status VARCHAR(30) DEFAULT 'active' COMMENT '状态',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  role_code VARCHAR(80) NOT NULL COMMENT '角色编码',
  role_name VARCHAR(120) NOT NULL COMMENT '角色名称',
  status VARCHAR(30) DEFAULT 'active' COMMENT '状态',
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
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_role_data_scope (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  scope_type VARCHAR(30) DEFAULT 'all' COMMENT 'all/platform/advertiser/agent',
  advertiser_id BIGINT DEFAULT NULL,
  agent_id BIGINT DEFAULT NULL,
  region_code VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_sys_role_scope_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据权限表';

INSERT IGNORE INTO sys_role (id, role_code, role_name, remark) VALUES
(1, 'super_admin', '超级管理员', '拥有全部菜单和权限'),
(2, 'platform_operator', '平台运营', '负责广告、素材、计划、设备和下发'),
(3, 'finance', '财务人员', '负责报表和账单结算'),
(4, 'workorder_staff', '客服/工单人员', '负责设备、下发和工单处理');

INSERT IGNORE INTO sys_user (id, username, password, real_name, user_type, status) VALUES
(1, 'admin', 'admin123', '系统管理员', 'platform', 'active'),
(2, 'operator', 'admin123', '平台运营', 'platform', 'active'),
(3, 'finance', 'admin123', '财务人员', 'platform', 'active'),
(4, 'service', 'admin123', '工单客服', 'platform', 'active');

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3), (4, 4);

INSERT IGNORE INTO sys_menu (id, menu_name, menu_path, permission_code, icon, sort_no) VALUES
(1, '工作台', '/dashboard', 'dashboard:view', 'DataBoard', 10),
(2, '广告列表', '/ads', 'ad:view', 'Promotion', 20),
(3, '素材管理', '/materials', 'material:view', 'Picture', 30),
(4, '投放计划', '/plans', 'plan:view', 'Calendar', 40),
(5, '设备管理', '/devices', 'device:view', 'Monitor', 50),
(6, '下发记录', '/deliveries', 'delivery:view', 'Connection', 60),
(7, '数据报表', '/reports/overview', 'report:view', 'TrendCharts', 70),
(8, '账单结算', '/finance/bills', 'bill:view', 'Wallet', 80),
(9, '工单反馈', '/workorders', 'workOrder:view', 'Service', 90),
(10, '广告主', '/partners/advertisers', 'advertiser:view', 'OfficeBuilding', 100),
(11, '代理商', '/partners/agents', 'agent:view', 'UserFilled', 110),
(12, '系统管理', '/system/users', 'system:user:view', 'Setting', 120);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 10), (2, 11),
(3, 1), (3, 7), (3, 8),
(4, 1), (4, 5), (4, 6), (4, 9);

INSERT IGNORE INTO sys_role_data_scope (role_id, scope_type) VALUES
(1, 'all'), (2, 'platform'), (3, 'platform'), (4, 'platform');

-- Button/action permissions for formal RBAC.
-- Use pseudo menu paths beginning with # so the backend can keep them out of the sidebar menu list.
INSERT IGNORE INTO sys_menu (id, menu_name, menu_path, permission_code, icon, sort_no) VALUES
(101, 'Ad Edit', '#ad_edit', 'ad:edit', NULL, 201),
(102, 'Ad Submit', '#ad_submit', 'ad:submit', NULL, 202),
(103, 'Ad Audit', '#ad_audit', 'ad:audit', NULL, 203),
(104, 'Material Edit', '#material_edit', 'material:edit', NULL, 211),
(105, 'Material Submit', '#material_submit', 'material:submit', NULL, 212),
(106, 'Material Audit', '#material_audit', 'material:audit', NULL, 213),
(107, 'Plan Edit', '#plan_edit', 'plan:edit', NULL, 221),
(108, 'Plan Schedule', '#plan_schedule', 'plan:schedule', NULL, 222),
(109, 'Plan Delivery', '#plan_delivery', 'plan:delivery', NULL, 223),
(110, 'Device Manage', '#device_manage', 'device:manage', NULL, 231),
(111, 'Delivery Operate', '#delivery_operate', 'delivery:operate', NULL, 241),
(112, 'Bill Confirm', '#bill_confirm', 'bill:confirm', NULL, 251),
(113, 'Bill Pay', '#bill_pay', 'bill:pay', NULL, 252),
(114, 'WorkOrder Operate', '#workorder_operate', 'workOrder:operate', NULL, 261),
(115, 'Partner Manage', '#partner_manage', 'partner:manage', NULL, 271);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES
(2, 101), (2, 102), (2, 104), (2, 105), (2, 107), (2, 108), (2, 109), (2, 110), (2, 111), (2, 115),
(3, 112), (3, 113),
(4, 110), (4, 111), (4, 114);
