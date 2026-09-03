package com.mrd.ad.business.device.mq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrd.ad.business.device.service.AdDeviceService;
import com.mysher.common.mq.core.ConsumFace;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ViiAdDeviceMqConsumer implements ConsumFace {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViiAdDeviceMqConsumer.class);
    private static final String ONLINE_DEVICE = "online_device";
    private static final String OFFLINE_DEVICE = "offline_device";

    private final ObjectMapper objectMapper;
    private final AdDeviceService adDeviceService;

    public ViiAdDeviceMqConsumer(ObjectMapper objectMapper, AdDeviceService adDeviceService) {
        this.objectMapper = objectMapper;
        this.adDeviceService = adDeviceService;
    }

    @Override
    public String consum(String msg) throws Exception {
        if (StringUtils.isBlank(msg)) {
            return "empty";
        }
        try {
            JsonNode root = objectMapper.readTree(msg);
            JsonNode head = root.path("head");
            JsonNode content = root.path("content");
            String businessKey = text(head, "business_key");
            String jid = StringUtils.defaultIfBlank(text(content, "jid"), text(head, "number"));
            String mzNumber = StringUtils.defaultIfBlank(text(head, "mzNumber"), parseMzNumber(jid));
            String deviceType = StringUtils.defaultIfBlank(text(head, "deviceType"), parseDeviceType(jid));
            Date eventTime = parseEventTime(content.path("eventTime"));

            if (StringUtils.isBlank(mzNumber)) {
                LOGGER.warn("ignore ViiAD device mq, mzNumber is blank, msg={}", msg);
                return "ignored";
            }

            if (ONLINE_DEVICE.equals(businessKey)) {
                adDeviceService.upsertViitalkOnlineDevice(mzNumber, jid, text(content, "ip"), deviceType, eventTime);
            } else if (OFFLINE_DEVICE.equals(businessKey)) {
                adDeviceService.updateViitalkOfflineDevice(mzNumber, eventTime);
            } else {
                LOGGER.debug("ignore ViiAD device mq, unsupported business_key={}, msg={}", businessKey, msg);
            }
            return "success";
        } catch (Exception e) {
            LOGGER.warn("consume ViiAD device mq failed, msg={}", msg, e);
            return "fail";
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Date parseEventTime(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new Date();
        }
        long timestamp = node.asLong(0L);
        return timestamp > 0L ? new Date(timestamp) : new Date();
    }

    private String parseMzNumber(String jid) {
        if (StringUtils.isBlank(jid)) {
            return null;
        }
        String value = jid.trim();
        int atIndex = value.indexOf('@');
        if (atIndex > 0) {
            return value.substring(0, atIndex);
        }
        int slashIndex = value.indexOf('/');
        return slashIndex > 0 ? value.substring(0, slashIndex) : value;
    }

    private String parseDeviceType(String jid) {
        if (StringUtils.isBlank(jid)) {
            return null;
        }
        int slashIndex = jid.lastIndexOf('/');
        return slashIndex > -1 ? jid.substring(slashIndex + 1) : null;
    }
}
