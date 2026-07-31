package com.mrd.ad.business.system.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class SysPasswordService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(storedPassword)) {
            return false;
        }
        if (isEncoded(storedPassword)) {
            return encoder.matches(rawPassword, storedPassword);
        }
        return StringUtils.equals(rawPassword, storedPassword);
    }

    public boolean isEncoded(String storedPassword) {
        return StringUtils.startsWith(storedPassword, "$2a$")
                || StringUtils.startsWith(storedPassword, "$2b$")
                || StringUtils.startsWith(storedPassword, "$2y$");
    }
}
