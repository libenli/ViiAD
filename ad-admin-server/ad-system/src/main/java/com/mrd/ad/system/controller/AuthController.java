package com.mrd.ad.system.controller;

import com.mrd.ad.business.system.dto.AuthCaptchaResponse;
import com.mrd.ad.business.system.dto.AuthCodeLoginRequest;
import com.mrd.ad.business.system.dto.AuthCodeSendRequest;
import com.mrd.ad.business.system.dto.AuthPasswordLoginRequest;
import com.mrd.ad.business.system.dto.AuthSelectRoleRequest;
import com.mrd.ad.business.system.dto.ChangePasswordRequest;
import com.mrd.ad.business.system.dto.ForgotPasswordCodeRequest;
import com.mrd.ad.business.system.dto.ForgotPasswordResetRequest;
import com.mrd.ad.business.system.dto.ForgotPasswordValidateRequest;
import com.mrd.ad.business.system.dto.ForgotPasswordValidateResult;
import com.mrd.ad.business.system.service.SysAuthService;
import com.mrd.ad.business.system.service.SysForgotPasswordService;
import com.mrd.ad.business.system.service.SysVerificationCodeService;
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
    private final SysVerificationCodeService verificationCodeService;
    private final SysForgotPasswordService forgotPasswordService;

    public AuthController(SysAuthService sysAuthService,
                          SysVerificationCodeService verificationCodeService,
                          SysForgotPasswordService forgotPasswordService) {
        this.sysAuthService = sysAuthService;
        this.verificationCodeService = verificationCodeService;
        this.forgotPasswordService = forgotPasswordService;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        return ApiResult.success(sysAuthService.login(params.get("username"), params.get("password")));
    }

    @GetMapping("/captcha")
    public ApiResult<AuthCaptchaResponse> captcha() {
        return ApiResult.success(verificationCodeService.createCaptcha());
    }

    @PostMapping("/login/password")
    public ApiResult<Map<String, Object>> passwordLogin(@RequestBody AuthPasswordLoginRequest request) {
        verificationCodeService.validateCaptcha(request.getCaptchaUuid(), request.getCaptchaCode());
        return ApiResult.success(sysAuthService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/verification-code/send")
    public ApiResult<Void> sendVerificationCode(@RequestBody AuthCodeSendRequest request) {
        verificationCodeService.sendLoginCode(request.getType(), request.getCountryCode(), request.getTarget(), request.getLocale());
        return ApiResult.success();
    }

    @PostMapping("/forgot-password/code")
    public ApiResult<Void> sendForgotPasswordCode(@RequestBody ForgotPasswordCodeRequest request) {
        forgotPasswordService.sendCode(request);
        return ApiResult.success();
    }

    @PostMapping("/forgot-password/validate")
    public ApiResult<ForgotPasswordValidateResult> validateForgotPasswordCode(@RequestBody ForgotPasswordValidateRequest request) {
        return ApiResult.success(forgotPasswordService.validateCode(request));
    }

    @PostMapping("/forgot-password/reset")
    public ApiResult<Void> resetForgotPassword(@RequestBody ForgotPasswordResetRequest request) {
        forgotPasswordService.resetPassword(request);
        return ApiResult.success();
    }

    @PostMapping("/login/code")
    public ApiResult<Map<String, Object>> codeLogin(@RequestBody AuthCodeLoginRequest request) {
        return ApiResult.success(sysAuthService.loginByVerificationCode(
                request.getType(),
                request.getCountryCode(),
                request.getTarget(),
                request.getCode()));
    }

    @PostMapping("/select-role")
    public ApiResult<Map<String, Object>> selectRole(@RequestBody AuthSelectRoleRequest request) {
        return ApiResult.success(sysAuthService.selectRole(request.getTempToken(), request.getRoleId()));
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
