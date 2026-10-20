package com.mrd.ad;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.mrd.ad.**.mapper")
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.mrd.ad")
public class AdAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdAdminApplication.class, args);
    }
}

