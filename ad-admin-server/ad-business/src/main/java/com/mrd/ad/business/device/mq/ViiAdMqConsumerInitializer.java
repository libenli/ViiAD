package com.mrd.ad.business.device.mq;

import com.mysher.common.mq.core.MqFaceFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ViiAdMqConsumerInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViiAdMqConsumerInitializer.class);

    private final ViiAdMqProperties properties;
    private final ViiAdDeviceMqConsumer consumer;
    private volatile boolean started;

    public ViiAdMqConsumerInitializer(ViiAdMqProperties properties, ViiAdDeviceMqConsumer consumer) {
        this.properties = properties;
        this.consumer = consumer;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            LOGGER.info("ViiAD MQ consumer disabled");
            return;
        }
        if (started) {
            return;
        }
        if (StringUtils.isBlank(properties.getViiadC2sRule())) {
            LOGGER.warn("ViiAD MQ consumer rule is blank");
            return;
        }
        try {
            MqFaceFactory.getInstanes().initParam(properties.getUsername(), properties.getPassword(), properties.getHost(), properties.getPort());
            MqFaceFactory.getInstanes().buildConsum(consumer, properties.getViiadC2sRule());
            started = true;
            LOGGER.info("ViiAD MQ consumer started, host={}, port={}, rule={}", properties.getHost(), properties.getPort(), properties.getViiadC2sRule());
        } catch (Exception e) {
            LOGGER.error("start ViiAD MQ consumer failed", e);
        }
    }
}
