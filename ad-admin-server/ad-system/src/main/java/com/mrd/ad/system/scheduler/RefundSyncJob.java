package com.mrd.ad.system.scheduler;

import com.mrd.ad.business.wallet.service.impl.RefundSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "mrd.pay.refund-sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RefundSyncJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefundSyncJob.class);

    private final RefundSyncService refundSyncService;

    @Value("${mrd.pay.refund-sync.batch-size:50}")
    private int batchSize;

    @Value("${mrd.pay.refund-sync.max-retries:20}")
    private int maxRetries;

    public RefundSyncJob(RefundSyncService refundSyncService) {
        this.refundSyncService = refundSyncService;
    }

    @Scheduled(initialDelayString = "${mrd.pay.refund-sync.initial-delay-ms:15000}",
            fixedDelayString = "${mrd.pay.refund-sync.fixed-delay-ms:60000}")
    public void syncRefunds() {
        List<Long> ids = refundSyncService.findRefundOrderIdsToSync(batchSize, maxRetries);
        for (Long id : ids) {
            try {
                refundSyncService.syncRefundOrder(id);
            } catch (Exception ex) {
                LOGGER.error("退款状态同步失败，refundId={}", id, ex);
            }
        }
    }
}