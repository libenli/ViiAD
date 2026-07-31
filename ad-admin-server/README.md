# 百万广告系统后台工程

正式版后台骨架，技术栈保留 Java 8、Spring Boot 2.6.x、MyBatis-Plus、MySQL 原有版本、Redis、Druid、Knife4j。

## 模块

- `ad-common`：统一返回、异常、基础对象
- `ad-logging`：日志注解与后续 AOP
- `ad-business`：广告、素材、计划、设备、报表、账单、工单业务
- `ad-scheduler`：定时任务
- `ad-system`：启动模块、认证、系统管理、配置

## 启动

```bash
mvn -pl ad-system -am spring-boot:run
```

默认地址：

```text
http://localhost:8080
```

