package com.mrd.ad.system.security;

import com.mrd.ad.business.system.service.SysAuthService;
import com.mrd.ad.common.annotation.RequiresPermission;
import com.mrd.ad.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class PermissionAspect {

    private final SysAuthService sysAuthService;

    public PermissionAspect(SysAuthService sysAuthService) {
        this.sysAuthService = sysAuthService;
    }

    @Around("@annotation(com.mrd.ad.common.annotation.RequiresPermission) || @within(com.mrd.ad.common.annotation.RequiresPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresPermission permission = resolvePermission(joinPoint);
        if (permission == null || sysAuthService.hasPermission(getToken(), permission.value())) {
            return joinPoint.proceed();
        }
        throw new BusinessException(403, "无操作权限");
    }

    private RequiresPermission resolvePermission(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresPermission permission = signature.getMethod().getAnnotation(RequiresPermission.class);
        if (permission != null) {
            return permission;
        }
        return joinPoint.getTarget().getClass().getAnnotation(RequiresPermission.class);
    }

    private String getToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        return request.getHeader("Authorization");
    }
}
