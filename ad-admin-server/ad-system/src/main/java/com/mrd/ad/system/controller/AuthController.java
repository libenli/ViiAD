package com.mrd.ad.system.controller;

import com.mrd.ad.business.system.dto.ChangePasswordRequest;
import com.mrd.ad.business.system.service.SysAuthService;
import com.mrd.ad.common.core.ApiResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysAuthService sysAuthService;

    public AuthController(SysAuthService sysAuthService) {
        this.sysAuthService = sysAuthService;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        return ApiResult.success(sysAuthService.login(params.get("username"), params.get("password")));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() {
        return ApiResult.success();
    }

    @GetMapping("/info")
    public ApiResult<Map<String, Object>> info(@RequestHeader(value = "Authorization", required = false) String token) {
        return ApiResult.success(sysAuthService.info(token));
    }

    @GetMapping("/menus")
    public ApiResult<List<String>> menus(@RequestHeader(value = "Authorization", required = false) String token) {
        return ApiResult.success(sysAuthService.menuPaths(token));
    }

    @PostMapping("/change-password")
    public ApiResult<Void> changePassword(@RequestHeader(value = "Authorization", required = false) String token,
                                          @Validated @RequestBody ChangePasswordRequest request) {
        sysAuthService.changePassword(token, request.getOldPassword(), request.getNewPassword());
        return ApiResult.success();
    }
}
