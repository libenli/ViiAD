-- ViiAD V1 账号类型标识与菜单规范调整。
-- 已部署测试库执行本补丁即可同步账号名称、菜单名称和菜单排序。

UPDATE sys_user SET username = 'business@mysher.com', real_name = '业务运营' WHERE id = 5;
UPDATE sys_user SET username = 'operator@mysher.com', real_name = '运维工程师' WHERE id = 3;
UPDATE sys_user SET username = 'reviewer@mysher.com', real_name = '素材审核员' WHERE id = 6;
UPDATE sys_user SET username = 'auditor@mysher.com', real_name = '审计人员' WHERE id = 12;

UPDATE sys_menu SET menu_name = '工作台', icon = 'DataBoard', sort_no = 10 WHERE id = 1;
UPDATE sys_menu SET menu_name = '用户管理', icon = 'User', sort_no = 20 WHERE id = 12;
UPDATE sys_menu SET menu_name = '权限管理', icon = 'Setting', sort_no = 30 WHERE id = 13;
UPDATE sys_menu SET menu_name = '设备管理', icon = 'Monitor', sort_no = 40 WHERE id = 5;
UPDATE sys_menu SET menu_name = '代理商', icon = 'UserFilled', sort_no = 50 WHERE id = 11;
UPDATE sys_menu SET menu_name = '广告主', icon = 'OfficeBuilding', sort_no = 60 WHERE id = 10;
UPDATE sys_menu SET menu_name = '广告列表', icon = 'Promotion', sort_no = 70 WHERE id = 2;
UPDATE sys_menu SET menu_name = '素材管理', icon = 'Picture', sort_no = 80 WHERE id = 3;
UPDATE sys_menu SET menu_name = '投放计划', icon = 'Calendar', sort_no = 90 WHERE id = 4;
UPDATE sys_menu SET menu_name = '下发记录', icon = 'Connection', sort_no = 100 WHERE id = 6;
UPDATE sys_menu SET menu_name = '工单反馈', icon = 'Service', sort_no = 110 WHERE id = 9;
UPDATE sys_menu SET menu_name = '数据报表', icon = 'TrendCharts', sort_no = 120 WHERE id = 7;
UPDATE sys_menu SET menu_name = '账单结算', icon = 'Wallet', sort_no = 130 WHERE id = 8;
UPDATE sys_menu SET menu_name = '操作日志', icon = 'Document', sort_no = 140 WHERE id = 14;
