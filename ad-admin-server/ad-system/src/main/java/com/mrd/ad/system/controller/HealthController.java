package com.mrd.ad.system.controller;

import com.mrd.ad.common.core.ApiResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("status", "UP");
        data.put("service", "ad-admin-server");
        data.put("java", "8");
        return ApiResult.success(data);
    }
}

