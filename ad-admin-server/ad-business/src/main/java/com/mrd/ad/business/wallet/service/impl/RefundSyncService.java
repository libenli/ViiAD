package com.mrd.ad.business.wallet.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrd.ad.business.wallet.config.AdPayProperties;
import com.mrd.ad.business.wallet.domain.AdRefundOrder;
import com.mrd.ad.business.wallet.domain.AdWalletAccount;
import com.mrd.ad.business.wallet.domain.AdWalletTransaction;
import com.mrd.ad.business.wallet.mapper.AdRefundOrderMapper;
import com.mrd.ad.business.wallet.mapper.AdWalletAccountMapper;
import com.mrd.ad.business.wallet.mapper.AdWalletTransactionMapper;
import com.mrd.ad.business.wallet.pay.WechatPayHelper;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RefundSyncService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String RECONCILE_PENDING = "pending";
    private static final String RECONCILE_MATCHED = "matched";
    private static final String RECONCILE_EXCEPTION = "exception";

    private final AdRefundOrderMapper refundOrderMapper;
    private final AdWalletAccountMapper accountMapper;
    private final AdWalletTransactionMapper transactionMapper;
    private final AdPayProperties payProperties;

    public RefundSyncService(AdRefundOrderMapper refundOrderMapper,
                             AdWalletAccountMapper accountMapper,
                             AdWalletTransactionMapper transactionMapper,
                             AdPayProperties payProperties) {
        this.refundOrderMapper = refundOrderMapper;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.payProperties = payProperties;
    }

    public List<Long> findRefundOrderIdsToSync(int limit, int maxRetries) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        int safeRetries = Math.max(1, maxRetries);
        List<Long> ids = new ArrayList<Long>();
        List<AdRefundOrder> processing = refundOrderMapper.selectList(new LambdaQueryWrapper<AdRefundOrder>()
                .eq(AdRefundOrder::getDeleted, 0)
                .eq(AdRefundOrder::getStatus, "processing")
                .and(wrapper -> wrapper.isNull(AdRefundOrder::getSyncRetryCount)
                        .or().lt(AdRefundOrder::getSyncRetryCount, safeRetries))
                .orderByAsc(AdRefundOrder::getLastSyncTime)
                .orderByAsc(AdRefundOrder::getId)
                .last("LIMIT " + safeLimit));
        for (AdRefundOrder item : processing) {
            ids.add(item.getId());
        }
        int remaining = safeLimit - ids.size();
        if (remaining <= 0) {
            return ids;
        }
        List<AdRefundOrder> unreconciled = refundOrderMapper.selectList(new LambdaQueryWrapper<AdRefundOrder>()
                .eq(AdRefundOrder::getDeleted, 0)
                .eq(AdRefundOrder::getStatus, "refunded")
                .and(wrapper -> wrapper.isNull(AdRefundOrder::getReconcileStatus)
                        .or().ne(AdRefundOrder::getReconcileStatus, RECONCILE_MATCHED))
                .and(wrapper -> wrapper.isNull(AdRefundOrder::getSyncRetryCount)
                        .or().lt(AdRefundOrder::getSyncRetryCount, safeRetries))
                .orderByAsc(AdRefundOrder::getLastSyncTime)
                .orderByAsc(AdRefundOrder::getId)
                .last("LIMIT " + remaining));
        for (AdRefundOrder item : unreconciled) {
            ids.add(item.getId());
        }
        return ids;
    }

    @Transactional(rollbackFor = Exception.class)
    public AdRefundOrder syncRefundOrder(Long id) {
        AdRefundOrder refund = lockedRefund(id);
        if (Arrays.asList("auditing", "rejected", "cancelled").contains(refund.getStatus())) {
            throw new BusinessException("当前退款状态不允许同步");
        }
        refund.setLastSyncTime(new Date());
        refund.setSyncRetryCount((refund.getSyncRetryCount() == null ? 0 : refund.getSyncRetryCount()) + 1);
        refund.setUpdateTime(new Date());
        refundOrderMapper.updateById(refund);

        RefundQueryResult result = queryGateway(refund);
        refund.setGatewayRefundStatus(StringUtils.abbreviate(result.gatewayStatus, 50));
        refund.setLastSyncTime(new Date());
        refund.setReconcileMessage(StringUtils.abbreviate(result.message, 500));
        if (StringUtils.isNotBlank(result.refundTransactionNo)) {
            refund.setRefundTransactionNo(result.refundTransactionNo);
        }

        if (RefundQueryResult.SUCCESS.equals(result.state)) {
            return applyGatewaySuccess(refund, result);
        }
        if (RefundQueryResult.FAILED.equals(result.state)) {
            if ("refunded".equals(refund.getStatus())) {
                refund.setReconcileStatus(RECONCILE_EXCEPTION);
                refund.setReconcileMessage(StringUtils.abbreviate("本地已退款，但支付平台状态为失败：" + result.message, 500));
                refund.setUpdateTime(new Date());
                refundOrderMapper.updateById(refund);
                return refund;
            }
            return releaseReservation(refund, result.gatewayStatus, result.message, RECONCILE_MATCHED);
        }

        if (!"refunded".equals(refund.getStatus()) && !"failed".equals(refund.getStatus())) {
            refund.setStatus("processing");
        }
        refund.setReconcileStatus(RefundQueryResult.PROCESSING.equals(result.state) && !"failed".equals(refund.getStatus())
                ? RECONCILE_PENDING : RECONCILE_EXCEPTION);
        refund.setFailReason(RefundQueryResult.UNKNOWN.equals(result.state) ? StringUtils.abbreviate(result.message, 500) : null);
        refund.setUpdateTime(new Date());
        refundOrderMapper.updateById(refund);
        return refund;
    }

    @Transactional(rollbackFor = Exception.class)
    public AdRefundOrder markSubmissionFailed(Long id, String gatewayStatus, String message, boolean uncertain) {
        AdRefundOrder refund = lockedRefund(id);
        if (uncertain) {
            refund.setStatus("processing");
            refund.setGatewayRefundStatus(StringUtils.abbreviate(gatewayStatus, 50));
            refund.setReconcileStatus(RECONCILE_EXCEPTION);
            refund.setReconcileMessage(StringUtils.abbreviate(message, 500));
            refund.setFailReason(StringUtils.abbreviate(message, 500));
            refund.setLastSyncTime(new Date());
            refund.setUpdateTime(new Date());
            refundOrderMapper.updateById(refund);
            return refund;
        }
        return releaseReservation(refund, gatewayStatus, message, RECONCILE_MATCHED);
    }

    private AdRefundOrder applyGatewaySuccess(AdRefundOrder refund, RefundQueryResult result) {
        BigDecimal expected = money(refund.getRefundAmount());
        if (result.refundAmount != null && money(result.refundAmount).compareTo(expected) != 0) {
            refund.setStatus("refunded");
            refund.setReconcileStatus(RECONCILE_EXCEPTION);
            refund.setReconcileMessage("支付平台退款金额与本地申请金额不一致：平台 "
                    + money(result.refundAmount).toPlainString() + "，本地 " + expected.toPlainString());
            refund.setRefundTime(result.refundTime == null ? new Date() : result.refundTime);
            refund.setUpdateTime(new Date());
            refundOrderMapper.updateById(refund);
            return refund;
        }

        List<AdWalletTransaction> transactions = refundTransactions(refund.getId());
        if ("refunded".equals(refund.getStatus())) {
            return reconcileCompletedRefund(refund, result, transactions);
        }
        if (!transactions.isEmpty()) {
            refund.setStatus("refunded");
            refund.setReconcileStatus(RECONCILE_EXCEPTION);
            refund.setReconcileMessage("发现退款资金流水，但退款单尚未完成，请财务核查");
            refund.setUpdateTime(new Date());
            refundOrderMapper.updateById(refund);
            return refund;
        }

        AdWalletAccount account = lockedAccount(refund.getAccountId());
        BigDecimal amount = expected;
        BigDecimal availableBefore = money(account.getAvailableBalance());
        BigDecimal frozenBefore = money(account.getFrozenBalance());
        BigDecimal reserved = money(refund.getReservedAmount());
        BigDecimal availableAfter = availableBefore;
        BigDecimal frozenAfter = frozenBefore;
        if (reserved.compareTo(ZERO) > 0) {
            if (frozenBefore.compareTo(reserved) < 0) {
                return markSettlementException(refund, result, "支付平台已退款，但账户冻结金额不足，需人工调整");
            }
            frozenAfter = frozenBefore.subtract(reserved);
        } else {
            if (availableBefore.compareTo(amount) < 0) {
                return markSettlementException(refund, result, "支付平台已退款，但账户可用余额不足，需人工调整");
            }
            availableAfter = availableBefore.subtract(amount);
        }

        account.setAvailableBalance(availableAfter);
        account.setFrozenBalance(frozenAfter);
        account.setTotalRecharge(money(account.getTotalRecharge()).subtract(amount).max(ZERO));
        account.setUpdateTime(new Date());
        accountMapper.updateById(account);

        insertRefundTransaction(account, refund, amount, availableBefore, availableAfter, frozenBefore, frozenAfter);
        refund.setStatus("refunded");
        refund.setReservedAmount(ZERO);
        refund.setRefundTime(result.refundTime == null ? new Date() : result.refundTime);
        refund.setFailReason(null);
        refund.setReconcileStatus(RECONCILE_MATCHED);
        refund.setReconcileMessage(StringUtils.abbreviate(result.message, 500));
        refund.setUpdateTime(new Date());
        refundOrderMapper.updateById(refund);
        return refund;
    }

    private AdRefundOrder reconcileCompletedRefund(AdRefundOrder refund, RefundQueryResult result,
                                                    List<AdWalletTransaction> transactions) {
        if (transactions.size() != 1) {
            refund.setReconcileStatus(RECONCILE_EXCEPTION);
            refund.setReconcileMessage(transactions.isEmpty()
                    ? "支付平台退款成功，但本地退款资金流水缺失"
                    : "支付平台退款成功，但本地存在重复退款资金流水");
        } else if (money(transactions.get(0).getAmount()).compareTo(money(refund.getRefundAmount())) != 0) {
            refund.setReconcileStatus(RECONCILE_EXCEPTION);
            refund.setReconcileMessage("退款单金额与本地资金流水金额不一致");
        } else if (money(refund.getReservedAmount()).compareTo(ZERO) != 0) {
            refund.setReconcileStatus(RECONCILE_EXCEPTION);
            refund.setReconcileMessage("退款已完成，但仍存在未释放的冻结金额");
        } else {
            refund.setReconcileStatus(RECONCILE_MATCHED);
            refund.setReconcileMessage(StringUtils.abbreviate(result.message, 500));
        }
        refund.setFailReason(null);
        refund.setUpdateTime(new Date());
        refundOrderMapper.updateById(refund);
        return refund;
    }

    private AdRefundOrder markSettlementException(AdRefundOrder refund, RefundQueryResult result, String message) {
        refund.setStatus("refunded");
        refund.setRefundTime(result.refundTime == null ? new Date() : result.refundTime);
        refund.setReconcileStatus(RECONCILE_EXCEPTION);
        refund.setReconcileMessage(message);
        refund.setFailReason(message);
        refund.setUpdateTime(new Date());
        refundOrderMapper.updateById(refund);
        return refund;
    }

    private AdRefundOrder releaseReservation(AdRefundOrder refund, String gatewayStatus, String message, String reconcileStatus) {
        BigDecimal reserved = money(refund.getReservedAmount());
        if (reserved.compareTo(ZERO) > 0) {
            AdWalletAccount account = lockedAccount(refund.getAccountId());
            BigDecimal frozen = money(account.getFrozenBalance());
            if (frozen.compareTo(reserved) < 0) {
                refund.setStatus("failed");
                refund.setGatewayRefundStatus(StringUtils.abbreviate(gatewayStatus, 50));
                refund.setReconcileStatus(RECONCILE_EXCEPTION);
                refund.setReconcileMessage("退款失败，但账户冻结金额不足，无法自动释放：" + message);
                refund.setFailReason(StringUtils.abbreviate(message, 500));
                refund.setUpdateTime(new Date());
                refundOrderMapper.updateById(refund);
                return refund;
            }
            account.setAvailableBalance(money(account.getAvailableBalance()).add(reserved));
            account.setFrozenBalance(frozen.subtract(reserved));
            account.setUpdateTime(new Date());
            accountMapper.updateById(account);
        }
        refund.setStatus("failed");
        refund.setReservedAmount(ZERO);
        refund.setGatewayRefundStatus(StringUtils.abbreviate(gatewayStatus, 50));
        refund.setReconcileStatus(reconcileStatus);
        refund.setReconcileMessage(StringUtils.abbreviate(message, 500));
        refund.setFailReason(StringUtils.abbreviate(message, 500));
        refund.setUpdateTime(new Date());
        refundOrderMapper.updateById(refund);
        return refund;
    }

    private RefundQueryResult queryGateway(AdRefundOrder refund) {
        if ("wechat".equals(refund.getPayMethod())) {
            return queryWechatRefund(refund);
        }
        if ("alipay".equals(refund.getPayMethod())) {
            return queryAlipayRefund(refund);
        }
        return RefundQueryResult.success("MANUAL_SUCCESS", refund.getRefundTransactionNo(),
                refund.getRefundAmount(), new Date(), "人工退款已确认");
    }

    private RefundQueryResult queryWechatRefund(AdRefundOrder refund) {
        AdPayProperties.Wechat config = payProperties.getWechat();
        if (config == null || !config.isEnabled() || StringUtils.isAnyBlank(config.getAppId(), config.getMchId(), config.getApiKey(), config.getRefundQueryUrl())) {
            return RefundQueryResult.unknown("CONFIG_ERROR", "微信退款查询配置不完整");
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put("appid", config.getAppId());
        params.put("mch_id", config.getMchId());
        params.put("nonce_str", WechatPayHelper.nonce());
        params.put("out_refund_no", refund.getRefundNo());
        params.put("sign", WechatPayHelper.sign(params, config.getApiKey()));
        try {
            String responseXml = WechatPayHelper.postXml(config.getRefundQueryUrl(), WechatPayHelper.toXml(params));
            if (StringUtils.isBlank(responseXml) || !responseXml.trim().startsWith("<xml")) {
                return RefundQueryResult.unknown("INVALID_RESPONSE", "微信退款查询返回内容不是有效XML");
            }
            Map<String, String> response = WechatPayHelper.parseXml(responseXml);
            if (StringUtils.isNotBlank(response.get("sign")) && !WechatPayHelper.verify(response, config.getApiKey())) {
                return RefundQueryResult.unknown("SIGN_ERROR", "微信退款查询响应验签失败");
            }
            if (!"SUCCESS".equals(response.get("return_code"))) {
                return RefundQueryResult.unknown("COMM_ERROR", StringUtils.defaultIfBlank(response.get("return_msg"), "微信退款查询通信失败"));
            }
            if (!"SUCCESS".equals(response.get("result_code"))) {
                return RefundQueryResult.unknown(StringUtils.defaultIfBlank(response.get("err_code"), "QUERY_FAILED"),
                        StringUtils.defaultIfBlank(response.get("err_code_des"), "微信退款记录暂未查询到"));
            }
            String status = StringUtils.defaultIfBlank(response.get("refund_status_0"), "UNKNOWN");
            String refundId = StringUtils.defaultIfBlank(response.get("refund_id_0"), refund.getRefundTransactionNo());
            BigDecimal amount = centToYuan(response.get("refund_fee_0"));
            if ("SUCCESS".equals(status)) {
                return RefundQueryResult.success(status, refundId, amount, new Date(), "微信退款成功");
            }
            if ("PROCESSING".equals(status)) {
                return RefundQueryResult.processing(status, refundId, "微信退款处理中");
            }
            if ("REFUNDCLOSE".equals(status) || "CHANGE".equals(status)) {
                return RefundQueryResult.failed(status, refundId, "微信退款失败：" + status);
            }
            return RefundQueryResult.unknown(status, "未知微信退款状态：" + status);
        } catch (Exception ex) {
            return RefundQueryResult.unknown("QUERY_EXCEPTION", "微信退款查询异常：" + ex.getMessage());
        }
    }

    private RefundQueryResult queryAlipayRefund(AdRefundOrder refund) {
        AdPayProperties.Alipay config = payProperties.getAlipay();
        if (config == null || !config.isEnabled()
                || StringUtils.isAnyBlank(config.getAppId(), config.getMerchantPrivateKey(), config.getAlipayPublicKey(), config.getGatewayUrl())) {
            return RefundQueryResult.unknown("CONFIG_ERROR", "支付宝退款查询配置不完整");
        }
        AlipayClient client = new DefaultAlipayClient(config.getGatewayUrl(), config.getAppId(), config.getMerchantPrivateKey(),
                "json", config.getCharset(), config.getAlipayPublicKey(), config.getSignType());
        AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
        Map<String, Object> biz = new HashMap<String, Object>();
        biz.put("out_trade_no", refund.getRechargeNo());
        if (StringUtils.isNotBlank(refund.getTransactionNo())) {
            biz.put("trade_no", refund.getTransactionNo());
        }
        biz.put("out_request_no", refund.getRefundNo());
        biz.put("query_options", Arrays.asList("gmt_refund_pay"));
        request.setBizContent(JSON.toJSONString(biz));
        try {
            AlipayTradeFastpayRefundQueryResponse response = client.execute(request);
            if (!response.isSuccess()) {
                return RefundQueryResult.unknown(StringUtils.defaultIfBlank(response.getSubCode(), response.getCode()),
                        "支付宝退款记录暂未查询到：" + StringUtils.defaultIfBlank(response.getSubMsg(), response.getMsg()));
            }
            String status = StringUtils.defaultIfBlank(response.getRefundStatus(), "UNKNOWN");
            BigDecimal amount = parseAmount(response.getRefundAmount());
            if ("REFUND_SUCCESS".equals(status)) {
                return RefundQueryResult.success(status,
                        StringUtils.defaultIfBlank(response.getTradeNo(), refund.getRefundTransactionNo()), amount,
                        response.getGmtRefundPay(), "支付宝退款成功");
            }
            return RefundQueryResult.processing(status, response.getTradeNo(), "支付宝退款处理中：" + status);
        } catch (AlipayApiException ex) {
            return RefundQueryResult.unknown("QUERY_EXCEPTION", "支付宝退款查询异常：" + ex.getMessage());
        }
    }

    private AdRefundOrder lockedRefund(Long id) {
        AdRefundOrder refund = refundOrderMapper.selectOne(new LambdaQueryWrapper<AdRefundOrder>()
                .eq(AdRefundOrder::getId, id)
                .eq(AdRefundOrder::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (refund == null) {
            throw new BusinessException(404, "退款申请不存在");
        }
        return refund;
    }

    private AdWalletAccount lockedAccount(Long id) {
        AdWalletAccount account = accountMapper.selectOne(new LambdaQueryWrapper<AdWalletAccount>()
                .eq(AdWalletAccount::getId, id)
                .eq(AdWalletAccount::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account == null) {
            throw new BusinessException("资金账户不存在");
        }
        return account;
    }

    private List<AdWalletTransaction> refundTransactions(Long refundId) {
        return transactionMapper.selectList(new LambdaQueryWrapper<AdWalletTransaction>()
                .eq(AdWalletTransaction::getBizType, "refund_order")
                .eq(AdWalletTransaction::getBizId, refundId));
    }

    private void insertRefundTransaction(AdWalletAccount account, AdRefundOrder refund, BigDecimal amount,
                                         BigDecimal availableBefore, BigDecimal availableAfter,
                                         BigDecimal frozenBefore, BigDecimal frozenAfter) {
        AdWalletTransaction transaction = new AdWalletTransaction();
        transaction.setTransactionNo(nextTransactionNo());
        transaction.setAccountId(account.getId());
        transaction.setOwnerType(account.getOwnerType());
        transaction.setOwnerId(account.getOwnerId());
        transaction.setTransactionType("refund");
        transaction.setDirection("out");
        transaction.setAmount(amount);
        transaction.setBalanceBefore(availableBefore);
        transaction.setBalanceAfter(availableAfter);
        transaction.setFrozenBefore(frozenBefore);
        transaction.setFrozenAfter(frozenAfter);
        transaction.setBizType("refund_order");
        transaction.setBizId(refund.getId());
        transaction.setRemark("充值退款：" + refund.getRefundNo());
        transaction.setCreateTime(new Date());
        transactionMapper.insert(transaction);
    }

    private String nextTransactionNo() {
        return "WT" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private BigDecimal centToYuan(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseAmount(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? ZERO : value).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private static class RefundQueryResult {
        private static final String SUCCESS = "success";
        private static final String PROCESSING = "processing";
        private static final String FAILED = "failed";
        private static final String UNKNOWN = "unknown";

        private final String state;
        private final String gatewayStatus;
        private final String refundTransactionNo;
        private final BigDecimal refundAmount;
        private final Date refundTime;
        private final String message;

        private RefundQueryResult(String state, String gatewayStatus, String refundTransactionNo,
                                  BigDecimal refundAmount, Date refundTime, String message) {
            this.state = state;
            this.gatewayStatus = gatewayStatus;
            this.refundTransactionNo = refundTransactionNo;
            this.refundAmount = refundAmount;
            this.refundTime = refundTime;
            this.message = message;
        }

        private static RefundQueryResult success(String gatewayStatus, String refundTransactionNo,
                                                 BigDecimal refundAmount, Date refundTime, String message) {
            return new RefundQueryResult(SUCCESS, gatewayStatus, refundTransactionNo, refundAmount, refundTime, message);
        }

        private static RefundQueryResult processing(String gatewayStatus, String refundTransactionNo, String message) {
            return new RefundQueryResult(PROCESSING, gatewayStatus, refundTransactionNo, null, null, message);
        }

        private static RefundQueryResult failed(String gatewayStatus, String refundTransactionNo, String message) {
            return new RefundQueryResult(FAILED, gatewayStatus, refundTransactionNo, null, null, message);
        }

        private static RefundQueryResult unknown(String gatewayStatus, String message) {
            return new RefundQueryResult(UNKNOWN, gatewayStatus, null, null, null, message);
        }
    }
}