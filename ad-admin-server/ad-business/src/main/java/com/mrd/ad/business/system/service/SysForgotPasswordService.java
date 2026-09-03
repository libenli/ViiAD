package com.mrd.ad.business.system.service;

import com.mrd.ad.business.system.domain.SysUser;
import com.mrd.ad.business.system.dto.ForgotPasswordCodeRequest;
import com.mrd.ad.business.system.dto.ForgotPasswordResetRequest;
import com.mrd.ad.business.system.dto.ForgotPasswordValidateRequest;
import com.mrd.ad.business.system.dto.ForgotPasswordValidateResult;
import com.mrd.ad.business.system.mapper.SysUserMapper;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysForgotPasswordService {

    private final SysVerificationCodeService verificationCodeService;
    private final SysPasswordService passwordService;
    private final SysUserMapper sysUserMapper;

    public SysForgotPasswordService(SysVerificationCodeService verificationCodeService,
                                    SysPasswordService passwordService,
                                    SysUserMapper sysUserMapper) {
        this.verificationCodeService = verificationCodeService;
        this.passwordService = passwordService;
        this.sysUserMapper = sysUserMapper;
    }

    public void sendCode(ForgotPasswordCodeRequest request) {
        verificationCodeService.sendForgotPasswordCode(
                request.getType(),
                request.getCountryCode(),
                request.getTarget(),
                request.getLocale());
    }

    public ForgotPasswordValidateResult validateCode(ForgotPasswordValidateRequest request) {
        SysUser user = verificationCodeService.validateForgotPasswordCode(
                request.getType(),
                request.getCountryCode(),
                request.getTarget(),
                request.getCode());
        return new ForgotPasswordValidateResult(verificationCodeService.createPasswordResetToken(user.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ForgotPasswordResetRequest request) {
        Long userId = verificationCodeService.consumePasswordResetToken(request.getResetToken());
        if (StringUtils.isBlank(request.getPassword())) {
            throw new BusinessException("请输入新密码");
        }
        if (request.getPassword().length() < 6) {
            throw new BusinessException("新密码至少 6 位");
        }
        if (request.getPassword().length() > 50) {
            throw new BusinessException("密码最多只能输入50个字符");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getDeleted() != null && user.getDeleted() == 1 || !"active".equals(user.getStatus())) {
            throw new BusinessException("账号不存在或已停用");
        }
        user.setPassword(passwordService.encode(request.getPassword()));
        sysUserMapper.updateById(user);
    }
}
