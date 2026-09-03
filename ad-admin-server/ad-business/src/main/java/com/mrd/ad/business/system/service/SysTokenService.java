package com.mrd.ad.business.system.service;

import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Service
public class SysTokenService {

    private static final String PREFIX = "mrd";
    private static final String TEMP_PREFIX = "mrdtmp";
    private static final String SECRET = "MRD_AD_ADMIN_2026_TOKEN_SECRET";
    private static final long EXPIRE_MILLIS = 8L * 60L * 60L * 1000L;
    private static final long TEMP_EXPIRE_MILLIS = 10L * 60L * 1000L;

    public String createToken(Long userId) {
        return createToken(userId, null);
    }

    public String createToken(Long userId, Long roleId) {
        long expireAt = System.currentTimeMillis() + EXPIRE_MILLIS;
        String rolePart = roleId == null ? "0" : String.valueOf(roleId);
        String payload = userId + "." + expireAt + "." + rolePart + "." + UUID.randomUUID().toString().replace("-", "");
        return PREFIX + "." + encode(payload) + "." + sign(payload);
    }

    public String createTempToken(Long userId) {
        long expireAt = System.currentTimeMillis() + TEMP_EXPIRE_MILLIS;
        String payload = userId + "." + expireAt + "." + UUID.randomUUID().toString().replace("-", "");
        return TEMP_PREFIX + "." + encode(payload) + "." + sign(payload);
    }

    public Long parseUserId(String token) {
        try {
            String value = normalize(token);
            if (StringUtils.startsWith(value, "rbac-")) {
                return parseLegacyToken(value);
            }
            String[] parts = splitToken(value, PREFIX, "登录已失效");
            return Long.valueOf(parsePayload(parts[1], parts[2], "登录已失效")[0]);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(401, "登录已失效");
        }
    }

    public Long parseRoleId(String token) {
        try {
            String value = normalize(token);
            if (StringUtils.startsWith(value, "rbac-")) {
                return null;
            }
            String[] parts = splitToken(value, PREFIX, "登录已失效");
            String[] payloadParts = parsePayload(parts[1], parts[2], "登录已失效");
            if (payloadParts.length < 4 || "0".equals(payloadParts[2])) {
                return null;
            }
            return Long.valueOf(payloadParts[2]);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(401, "登录已失效");
        }
    }

    public Long parseTempUserId(String tempToken) {
        try {
            String value = normalize(tempToken);
            String[] parts = splitToken(value, TEMP_PREFIX, "身份选择已失效，请重新登录");
            return Long.valueOf(parsePayload(parts[1], parts[2], "身份选择已失效，请重新登录")[0]);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(401, "身份选择已失效，请重新登录");
        }
    }

    private Long parseLegacyToken(String value) {
        String[] parts = value.split("-");
        if (parts.length < 3) {
            throw new BusinessException(401, "登录已失效");
        }
        return Long.valueOf(parts[1]);
    }

    private String normalize(String token) {
        if (StringUtils.isBlank(token)) {
            throw new BusinessException(401, "未登录");
        }
        return token.replace("Bearer ", "");
    }

    private String[] splitToken(String value, String expectedPrefix, String errorMessage) {
        String[] parts = value.split("\\.");
        if (parts.length != 3 || !expectedPrefix.equals(parts[0])) {
            throw new BusinessException(401, errorMessage);
        }
        return parts;
    }

    private String[] parsePayload(String encodedPayload, String signature, String errorMessage) {
        String payload = decode(encodedPayload);
        if (!StringUtils.equals(sign(payload), signature)) {
            throw new BusinessException(401, errorMessage);
        }
        String[] payloadParts = payload.split("\\.");
        if (payloadParts.length < 3) {
            throw new BusinessException(401, errorMessage);
        }
        long expireAt = Long.parseLong(payloadParts[1]);
        if (System.currentTimeMillis() > expireAt) {
            throw new BusinessException(401, errorMessage);
        }
        return payloadParts;
    }

    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(500, "Token签名失败");
        }
    }
}
