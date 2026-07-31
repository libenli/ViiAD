package com.mrd.ad.business.system.service;

import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class SysLoginSecurityService {

    private static final Logger log = LoggerFactory.getLogger(SysLoginSecurityService.class);

    private static final int MAX_FAIL_COUNT = 5;
    private static final long LOCK_MILLIS = 10L * 60L * 1000L;
    private static final long LOCK_SECONDS = LOCK_MILLIS / 1000L;
    private static final String FAIL_KEY_PREFIX = "mrd:login:fail:";
    private static final String LOCK_KEY_PREFIX = "mrd:login:lock:";

    private final Map<String, FailState> failStates = new ConcurrentHashMap<String, FailState>();
    private final StringRedisTemplate redisTemplate;

    public SysLoginSecurityService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkAllowed(String username) {
        try {
            String key = lockKey(username);
            Boolean locked = redisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(locked)) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                throw new BusinessException(429, lockMessage(ttl));
            }
            return;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Redis login lock check failed, fallback to local memory. username={}", username, ex);
            checkAllowedLocal(username);
        }
    }

    public void recordSuccess(String username) {
        try {
            redisTemplate.delete(failKey(username));
            redisTemplate.delete(lockKey(username));
            return;
        } catch (RuntimeException ex) {
            log.warn("Redis login failure cleanup failed, fallback to local memory. username={}", username, ex);
            failStates.remove(normalize(username));
        }
    }

    public void recordFailure(String username) {
        try {
            String failKey = failKey(username);
            Long failCount = redisTemplate.opsForValue().increment(failKey);
            if (failCount != null && failCount == 1L) {
                redisTemplate.expire(failKey, LOCK_SECONDS, TimeUnit.SECONDS);
            }
            if (failCount != null && failCount >= MAX_FAIL_COUNT) {
                redisTemplate.opsForValue().set(lockKey(username), "1", LOCK_SECONDS, TimeUnit.SECONDS);
                redisTemplate.delete(failKey);
            }
            return;
        } catch (RuntimeException ex) {
            log.warn("Redis login failure record failed, fallback to local memory. username={}", username, ex);
            recordFailureLocal(username);
        }
    }

    private void checkAllowedLocal(String username) {
        String key = normalize(username);
        FailState state = failStates.get(key);
        if (state == null || state.lockUntil == null) {
            return;
        }
        if (System.currentTimeMillis() < state.lockUntil) {
            throw new BusinessException(429, "登录失败次数过多，请稍后再试");
        }
        failStates.remove(key);
    }

    private void recordFailureLocal(String username) {
        String key = normalize(username);
        FailState state = failStates.get(key);
        if (state == null) {
            state = new FailState();
            failStates.put(key, state);
        }
        state.failCount++;
        if (state.failCount >= MAX_FAIL_COUNT) {
            state.lockUntil = System.currentTimeMillis() + LOCK_MILLIS;
        }
    }

    private String failKey(String username) {
        return FAIL_KEY_PREFIX + normalize(username);
    }

    private String lockKey(String username) {
        return LOCK_KEY_PREFIX + normalize(username);
    }

    private String normalize(String username) {
        return StringUtils.defaultString(username).trim().toLowerCase();
    }

    private String lockMessage(Long ttl) {
        if (ttl == null || ttl <= 0) {
            return "登录失败次数过多，请稍后再试";
        }
        long minutes = Math.max(1L, (ttl + 59L) / 60L);
        return "登录失败次数过多，请约 " + minutes + " 分钟后再试";
    }

    private static class FailState {
        private int failCount;
        private Long lockUntil;
    }
}
