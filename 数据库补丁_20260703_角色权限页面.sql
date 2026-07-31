-- Role permission page additions.
-- Run this after 数据库补丁_20260702_正式RBAC基础表.sql.

INSERT IGNORE INTO sys_menu (id, menu_name, menu_path, permission_code, icon, sort_no) VALUES
(13, 'Role Permission', '/system/roles', 'system:role:view', 'Setting', 130),
(116, 'Role Grant', '#system_role_grant', 'system:role:grant', NULL, 281),
(117, 'User Manage', '#system_user_manage', 'system:user:manage', NULL, 282);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE id IN (13, 116, 117);
