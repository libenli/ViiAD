package com.mrd.ad.business.device.service.impl;

import com.mrd.ad.business.device.dto.DeviceIpLocation;
import com.mrd.ad.business.device.service.DeviceIpLocationService;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;

@Service
public class DeviceIpLocationServiceImpl implements DeviceIpLocationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceIpLocationServiceImpl.class);
    private static final String IP_REGION_DB = "ip2region.db";

    private DbSearcher searcher;

    @PostConstruct
    public void init() {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(IP_REGION_DB)) {
            if (inputStream == null) {
                LOGGER.warn("ip2region database not found in classpath: {}", IP_REGION_DB);
                return;
            }
            this.searcher = new DbSearcher(new DbConfig(), toBytes(inputStream));
        } catch (Exception e) {
            LOGGER.warn("init ip2region searcher failed", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (searcher == null) {
            return;
        }
        try {
            searcher.close();
        } catch (Throwable e) {
            LOGGER.debug("close ip2region searcher ignored", e);
        } finally {
            searcher = null;
        }
    }

    @Override
    public DeviceIpLocation resolve(String ip) {
        String normalizedIp = normalizeIp(ip);
        if (StringUtils.isBlank(normalizedIp) || isInternalIp(normalizedIp) || searcher == null) {
            return unresolved(normalizedIp);
        }
        try {
            DataBlock dataBlock;
            synchronized (this) {
                dataBlock = searcher.memorySearch(normalizedIp);
            }
            if (dataBlock == null || StringUtils.isBlank(dataBlock.getRegion())) {
                return unresolved(normalizedIp);
            }
            return parseLocation(normalizedIp, dataBlock.getRegion());
        } catch (Exception e) {
            LOGGER.warn("resolve device ip location failed, ip={}", normalizedIp, e);
            return unresolved(normalizedIp);
        }
    }

    private byte[] toBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int len;
        while ((len = inputStream.read(buffer)) > -1) {
            outputStream.write(buffer, 0, len);
        }
        return outputStream.toByteArray();
    }

    private DeviceIpLocation parseLocation(String ip, String region) {
        String country = "";
        String province = "";
        String city = "";

        String[] parts = region.split("\\|");
        if (parts.length >= 5) {
            country = cleanRegionPart(parts[0]);
            province = normalizeProvince(cleanRegionPart(parts[2]));
            city = cleanRegionPart(parts[3]);
        } else if (parts.length >= 3) {
            country = cleanRegionPart(parts[0]);
            province = normalizeProvince(cleanRegionPart(parts[1]));
            city = cleanRegionPart(parts[2]);
        } else if (parts.length == 2) {
            country = cleanRegionPart(parts[0]);
            city = cleanRegionPart(parts[1]);
        }

        String regionName = buildRegion(country, province, city, region);
        boolean resolved = StringUtils.isNotBlank(country) || StringUtils.isNotBlank(province) || StringUtils.isNotBlank(city);
        return new DeviceIpLocation(ip, country, province, city, regionName, resolved);
    }

    private DeviceIpLocation unresolved(String ip) {
        return new DeviceIpLocation(ip, "", "", "", "", false);
    }

    private String normalizeIp(String ip) {
        if (StringUtils.isBlank(ip)) {
            return "";
        }
        String value = ip.trim();
        if (value.contains(",")) {
            value = value.split(",")[0].trim();
        }
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        int portIndex = value.lastIndexOf(':');
        if (portIndex > -1 && value.indexOf(':') == portIndex) {
            value = value.substring(0, portIndex);
        }
        return value;
    }

    private boolean isInternalIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress();
        } catch (Exception e) {
            return true;
        }
    }

    private String cleanRegionPart(String value) {
        if (StringUtils.isBlank(value) || "0".equals(value)) {
            return "";
        }
        return value.trim();
    }

    private String normalizeProvince(String province) {
        if (StringUtils.isBlank(province)) {
            return "";
        }
        return province.trim()
                .replace("壮族自治区", "")
                .replace("回族自治区", "")
                .replace("维吾尔自治区", "")
                .replace("特别行政区", "")
                .replace("自治区", "")
                .replace("省", "")
                .replace("市", "");
    }

    private String buildRegion(String country, String province, String city, String rawRegion) {
        StringBuilder region = new StringBuilder();
        appendRegion(region, country);
        appendRegion(region, province);
        appendRegion(region, city);
        if (region.length() > 0) {
            return region.toString();
        }
        return rawRegion == null ? "" : rawRegion;
    }

    private void appendRegion(StringBuilder region, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (region.length() > 0) {
            region.append(",");
        }
        region.append(value);
    }
}


