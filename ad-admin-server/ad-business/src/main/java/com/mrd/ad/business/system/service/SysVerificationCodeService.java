package com.mrd.ad.business.system.service;

import com.mrd.ad.business.system.domain.SysUser;
import com.mrd.ad.business.system.dto.AuthCaptchaResponse;
import com.mrd.ad.business.system.mapper.SysUserMapper;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class SysVerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(SysVerificationCodeService.class);

    private static final String CAPTCHA_PREFIX = "viiad:auth:captcha:";
    private static final String LOGIN_CODE_PREFIX = "viiad:auth:login-code:";
    private static final String LOGIN_CODE_LIMIT_PREFIX = "viiad:auth:login-code-limit:";
    private static final String FORGOT_CODE_PREFIX = "viiad:auth:forgot-code:";
    private static final String FORGOT_TOKEN_PREFIX = "viiad:auth:forgot-token:";
    private static final String TYPE_PHONE = "phone";
    private static final String TYPE_EMAIL = "email";

    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper sysUserMapper;
    private final SysVerificationMessageService messageService;
    private final Map<String, LocalValue> localValues = new ConcurrentHashMap<String, LocalValue>();
    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<String, LocalCounter>();

    @Value("${mrd.auth.captcha.expire-seconds:180}")
    private long captchaExpireSeconds;

    @Value("${mrd.auth.verification-code.expire-seconds:300}")
    private long codeExpireSeconds;

    @Value("${mrd.auth.verification-code.max-send-per-hour:5}")
    private int maxSendPerHour;

    @Value("${mrd.auth.forgot-password.reset-token-expire-seconds:300}")
    private long resetTokenExpireSeconds;

    public SysVerificationCodeService(StringRedisTemplate redisTemplate,
                                      SysUserMapper sysUserMapper,
                                      SysVerificationMessageService messageService) {
        this.redisTemplate = redisTemplate;
        this.sysUserMapper = sysUserMapper;
        this.messageService = messageService;
    }

    public AuthCaptchaResponse createCaptcha() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String code = RandomStringUtils.randomAlphanumeric(4).toLowerCase();
        storeValue(CAPTCHA_PREFIX + uuid, code, captchaExpireSeconds, "captcha");
        return new AuthCaptchaResponse(uuid, drawCaptcha(code));
    }

    public void validateCaptcha(String uuid, String code) {
        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(code)) {
            throw new BusinessException("Please enter image captcha");
        }
        String key = CAPTCHA_PREFIX + uuid;
        String saved = getAndDeleteValue(key, "captcha");
        if (StringUtils.isBlank(saved)) {
            throw new BusinessException("Image captcha expired");
        }
        if (!StringUtils.equalsIgnoreCase(saved, StringUtils.trim(code))) {
            throw new BusinessException("Image captcha is incorrect");
        }
    }

    public void sendLoginCode(String type, String countryCode, String target, String locale) {
        String normalizedType = normalizeType(type);
        String targetKey = buildTargetKey(normalizedType, countryCode, target);
        findBoundActiveUser(normalizedType, countryCode, target);
        checkSendLimit(targetKey);
        String code = RandomStringUtils.randomNumeric(6);
        storeValue(LOGIN_CODE_PREFIX + targetKey, code, codeExpireSeconds, "login verification code");
        increaseSendLimit(targetKey);
        messageService.sendLoginCode(normalizedType, countryCode, target, code, locale);
    }

    public void validateLoginCode(String type, String countryCode, String target, String code) {
        String targetKey = buildTargetKey(normalizeType(type), countryCode, target);
        String key = LOGIN_CODE_PREFIX + targetKey;
        String saved = getValue(key, "login verification code");
        if (StringUtils.isBlank(saved)) {
            throw new BusinessException("Verification code expired");
        }
        if (!StringUtils.equals(saved, StringUtils.trim(code))) {
            throw new BusinessException("Verification code is incorrect");
        }
        deleteValue(key, "login verification code");
    }

    public void sendForgotPasswordCode(String type, String countryCode, String target, String locale) {
        String normalizedType = normalizeType(type);
        String targetKey = buildTargetKey(normalizedType, countryCode, target);
        findBoundActiveUser(normalizedType, countryCode, target);
        checkSendLimit(targetKey);
        String code = RandomStringUtils.randomNumeric(6);
        storeValue(FORGOT_CODE_PREFIX + targetKey, code, codeExpireSeconds, "forgot password code");
        increaseSendLimit(targetKey);
        messageService.sendForgotPasswordCode(normalizedType, countryCode, target, code, locale);
    }

    public SysUser validateForgotPasswordCode(String type, String countryCode, String target, String code) {
        String normalizedType = normalizeType(type);
        SysUser user = findBoundActiveUser(normalizedType, countryCode, target);
        String targetKey = buildTargetKey(normalizedType, countryCode, target);
        String key = FORGOT_CODE_PREFIX + targetKey;
        String saved = getValue(key, "forgot password code");
        if (StringUtils.isBlank(saved)) {
            throw new BusinessException("验证码已失效，请重新获取");
        }
        if (!StringUtils.equals(saved, StringUtils.trim(code))) {
            throw new BusinessException("验证码错误，请重新输入");
        }
        deleteValue(key, "forgot password code");
        return user;
    }

    public String createPasswordResetToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        storeValue(FORGOT_TOKEN_PREFIX + token, String.valueOf(userId), resetTokenExpireSeconds, "forgot password reset token");
        return token;
    }

    public Long consumePasswordResetToken(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException("验证码已失效，请重新获取");
        }
        String value = getAndDeleteValue(FORGOT_TOKEN_PREFIX + token, "forgot password reset token");
        if (StringUtils.isBlank(value)) {
            throw new BusinessException("验证码已失效，请重新获取");
        }
        return Long.valueOf(value);
    }

    public String normalizeType(String type) {
        if (TYPE_PHONE.equals(type) || TYPE_EMAIL.equals(type)) {
            return type;
        }
        throw new BusinessException("Unsupported verification type");
    }

    public String buildTargetKey(String type, String countryCode, String target) {
        String normalizedTarget = StringUtils.trimToEmpty(target);
        if (StringUtils.isBlank(normalizedTarget)) {
            throw new BusinessException("Verification target cannot be empty");
        }
        if (TYPE_EMAIL.equals(type)) {
            return type + ":" + normalizedTarget.toLowerCase();
        }
        return type + ":" + StringUtils.defaultIfBlank(StringUtils.trim(countryCode), "+86") + ":" + normalizedTarget;
    }

    private void checkSendLimit(String targetKey) {
        String key = LOGIN_CODE_LIMIT_PREFIX + targetKey;
        String countValue;
        try {
            countValue = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            log.warn("Redis verification code limit check failed, fallback to local memory. targetKey={}", targetKey, ex);
            checkSendLimitLocal(key);
            return;
        }
        int count = StringUtils.isBlank(countValue) ? 0 : Integer.parseInt(countValue);
        if (count >= maxSendPerHour) {
            throw new BusinessException("Too many verification codes. Please try again later");
        }
    }

    private void increaseSendLimit(String targetKey) {
        String key = LOGIN_CODE_LIMIT_PREFIX + targetKey;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, 1, TimeUnit.HOURS);
            }
        } catch (RuntimeException ex) {
            log.warn("Redis verification code limit increase failed, fallback to local memory. targetKey={}", targetKey, ex);
            increaseSendLimitLocal(key);
        }
    }

    public SysUser findBoundActiveUser(String type, String countryCode, String target) {
        SysUser user = TYPE_EMAIL.equals(type)
                ? sysUserMapper.findActiveByEmailOrUsername(StringUtils.trim(target))
                : sysUserMapper.findActiveByPhone(StringUtils.defaultIfBlank(StringUtils.trim(countryCode), "+86"), StringUtils.trim(target));
        if (user == null) {
            throw new BusinessException("该手机号或邮箱未绑定任何后台账号，请核对后重新输入");
        }
        return user;
    }

    private String drawCaptcha(String code) {
        try {
            int width = 132;
            int height = 44;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(8, 24, 34));
            graphics.fillRect(0, 0, width, height);
            graphics.setColor(new Color(57, 198, 214, 80));
            graphics.setStroke(new BasicStroke(1.4f));
            graphics.drawLine(8, 34, 126, 11);
            graphics.drawLine(0, 14, 94, 42);
            graphics.setFont(new Font("SansSerif", Font.BOLD, 28));
            Color[] colors = new Color[] {
                    new Color(245, 181, 68),
                    new Color(74, 163, 255),
                    new Color(125, 221, 236),
                    new Color(99, 212, 144)
            };
            for (int i = 0; i < code.length(); i++) {
                graphics.setColor(colors[i % colors.length]);
                graphics.rotate(Math.toRadians(i % 2 == 0 ? -10 : 8), 28 + i * 22, 26);
                graphics.drawString(String.valueOf(code.charAt(i)), 18 + i * 25, 31);
                graphics.rotate(Math.toRadians(i % 2 == 0 ? 10 : -8), 28 + i * 22, 26);
            }
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException(500, "Failed to create captcha");
        }
    }

    private void storeValue(String key, String value, long expireSeconds, String label) {
        try {
            redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
        } catch (RuntimeException ex) {
            log.warn("Redis {} store failed, fallback to local memory. key={}", label, key, ex);
            localValues.put(key, new LocalValue(value, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expireSeconds)));
        }
    }

    private String getValue(String key, String label) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            log.warn("Redis {} read failed, fallback to local memory. key={}", label, key, ex);
            return getLocalValue(key, false);
        }
    }

    private String getAndDeleteValue(String key, String label) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            redisTemplate.delete(key);
            return value;
        } catch (RuntimeException ex) {
            log.warn("Redis {} read/delete failed, fallback to local memory. key={}", label, key, ex);
            return getLocalValue(key, true);
        }
    }

    private void deleteValue(String key, String label) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Redis {} delete failed, fallback to local memory. key={}", label, key, ex);
            localValues.remove(key);
        }
    }

    private String getLocalValue(String key, boolean remove) {
        LocalValue localValue = remove ? localValues.remove(key) : localValues.get(key);
        if (localValue == null) {
            return null;
        }
        if (System.currentTimeMillis() > localValue.expireAt) {
            localValues.remove(key);
            return null;
        }
        return localValue.value;
    }

    private void checkSendLimitLocal(String key) {
        LocalCounter counter = localCounters.get(key);
        if (counter == null || System.currentTimeMillis() > counter.expireAt) {
            localCounters.remove(key);
            return;
        }
        if (counter.count >= maxSendPerHour) {
            throw new BusinessException("Too many verification codes. Please try again later");
        }
    }

    private void increaseSendLimitLocal(String key) {
        LocalCounter counter = localCounters.get(key);
        long now = System.currentTimeMillis();
        if (counter == null || now > counter.expireAt) {
            counter = new LocalCounter(0, now + TimeUnit.HOURS.toMillis(1));
            localCounters.put(key, counter);
        }
        counter.count++;
    }

    private static class LocalValue {
        private final String value;
        private final long expireAt;

        private LocalValue(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private static class LocalCounter {
        private int count;
        private final long expireAt;

        private LocalCounter(int count, long expireAt) {
            this.count = count;
            this.expireAt = expireAt;
        }
    }
}
