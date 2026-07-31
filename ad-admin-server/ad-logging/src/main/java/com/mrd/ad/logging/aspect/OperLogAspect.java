package com.mrd.ad.logging.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrd.ad.business.system.domain.SysOperLog;
import com.mrd.ad.business.system.domain.SysUser;
import com.mrd.ad.business.system.mapper.SysOperLogMapper;
import com.mrd.ad.business.system.mapper.SysUserMapper;
import com.mrd.ad.business.system.service.SysTokenService;
import com.mrd.ad.logging.annotation.OperLog;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);
    private static final int MAX_TEXT_LENGTH = 3000;

    private final SysOperLogMapper operLogMapper;
    private final SysUserMapper userMapper;
    private final SysTokenService tokenService;
    private final ObjectMapper objectMapper;

    public OperLogAspect(SysOperLogMapper operLogMapper,
                         SysUserMapper userMapper,
                         SysTokenService tokenService,
                         ObjectMapper objectMapper) {
        this.operLogMapper = operLogMapper;
        this.userMapper = userMapper;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            saveLog(joinPoint, operLog, result, failure, System.currentTimeMillis() - start);
        }
    }

    private void saveLog(ProceedingJoinPoint joinPoint, OperLog operLog, Object result, Throwable failure, long costTime) {
        try {
            HttpServletRequest request = currentRequest();
            SysOperLog entity = new SysOperLog();
            entity.setModuleName(operLog.module());
            entity.setBusinessType(operLog.businessType());
            entity.setRequestUri(request == null ? null : request.getRequestURI());
            entity.setRequestMethod(request == null ? null : request.getMethod());
            entity.setOperatorName(resolveOperatorName(request));
            entity.setOperatorIp(resolveIp(request));
            entity.setRequestParam(toJson(safeArgs(joinPoint.getArgs())));
            entity.setResponseResult(failure == null ? toJson(result) : null);
            entity.setCostTime(costTime);
            entity.setStatus(failure == null ? 1 : 0);
            entity.setErrorMsg(failure == null ? null : truncate(failure.getMessage()));
            entity.setCreateTime(new Date());
            operLogMapper.insert(entity);
        } catch (Exception ex) {
            log.warn("save operation log failed module={}, businessType={}, error={}",
                    operLog.module(), operLog.businessType(), ex.getMessage());
        }
    }

    private List<Object> safeArgs(Object[] args) {
        List<Object> values = new ArrayList<Object>();
        if (args == null) {
            return values;
        }
        for (Object arg : args) {
            if (arg == null || arg instanceof HttpServletRequest || arg instanceof ServletResponse || arg instanceof MultipartFile) {
                continue;
            }
            values.add(arg);
        }
        return values;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return truncate(objectMapper.writeValueAsString(value));
        } catch (Exception ex) {
            return truncate(String.valueOf(value));
        }
    }

    private String resolveOperatorName(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            Long userId = tokenService.parseUserId(token);
            SysUser user = userMapper.selectById(userId);
            return user == null ? String.valueOf(userId) : StringUtils.defaultIfBlank(user.getUsername(), user.getRealName());
        } catch (Exception ex) {
            return null;
        }
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.defaultIfBlank(realIp, request.getRemoteAddr());
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) attributes).getRequest();
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_TEXT_LENGTH);
    }
}
