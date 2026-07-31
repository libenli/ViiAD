-- ViiAD V1 默认登录账号邮箱化。
-- 密码保持 admin123 不变；仅调整测试/演示默认账号的 username。

UPDATE sys_user SET username = 'admin@mysher.com' WHERE id = 1;
UPDATE sys_user SET username = 'developer@mysher.com' WHERE id = 2;
UPDATE sys_user SET username = 'operator@mysher.com' WHERE id = 3;
UPDATE sys_user SET username = 'region@mysher.com' WHERE id = 4;
UPDATE sys_user SET username = 'business@mysher.com' WHERE id = 5;
UPDATE sys_user SET username = 'reviewer@mysher.com' WHERE id = 6;
UPDATE sys_user SET username = 'finance@mysher.com' WHERE id = 7;
UPDATE sys_user SET username = 'analyst@mysher.com' WHERE id = 8;
UPDATE sys_user SET username = 'agent01@mysher.com' WHERE id = 9;
UPDATE sys_user SET username = 'advertiser01@mysher.com' WHERE id = 10;
UPDATE sys_user SET username = 'service@mysher.com' WHERE id = 11;
UPDATE sys_user SET username = 'auditor@mysher.com' WHERE id = 12;
UPDATE sys_user SET username = 'audience@mysher.com' WHERE id = 13;
