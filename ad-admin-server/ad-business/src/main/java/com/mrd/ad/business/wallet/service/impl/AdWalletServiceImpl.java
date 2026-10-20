package com.mrd.ad.business.wallet.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.mrd.ad.business.ad.domain.AdOrder;
import com.mrd.ad.business.ad.mapper.AdOrderMapper;
import com.mrd.ad.business.material.domain.AdMaterial;
import com.mrd.ad.business.material.mapper.AdMaterialMapper;
import com.mrd.ad.business.partner.domain.AdAdvertiser;
import com.mrd.ad.business.partner.domain.AdAgent;
import com.mrd.ad.business.partner.mapper.AdAdvertiserMapper;
import com.mrd.ad.business.partner.mapper.AdAgentMapper;
import com.mrd.ad.business.report.domain.AdPlayLog;
import com.mrd.ad.business.system.service.SysDataScopeService;
import com.mrd.ad.business.system.domain.SysUser;
import com.mrd.ad.business.wallet.domain.AdBillingRule;
import com.mrd.ad.business.wallet.domain.AdPayCallbackLog;
import com.mrd.ad.business.wallet.domain.AdPlayCharge;
import com.mrd.ad.business.wallet.domain.AdRechargeOrder;
import com.mrd.ad.business.wallet.domain.AdRefundOrder;
import com.mrd.ad.business.wallet.domain.AdRefundReconcileAction;
import com.mrd.ad.business.wallet.domain.AdWalletAccount;
import com.mrd.ad.business.wallet.domain.AdWalletTransaction;
import com.mrd.ad.business.wallet.config.AdPayProperties;
import com.mrd.ad.business.wallet.dto.BillingRuleQuery;
import com.mrd.ad.business.wallet.dto.BillingRuleSaveRequest;
import com.mrd.ad.business.wallet.dto.ConfirmRechargeRequest;
import com.mrd.ad.business.wallet.dto.AuditRefundOrderRequest;
import com.mrd.ad.business.wallet.dto.CreateRechargeOrderRequest;
import com.mrd.ad.business.wallet.dto.CreateRefundOrderRequest;
import com.mrd.ad.business.wallet.dto.PayCallbackLogQuery;
import com.mrd.ad.business.wallet.dto.PlayChargeQuery;
import com.mrd.ad.business.wallet.dto.RechargePaymentResult;
import com.mrd.ad.business.wallet.dto.RechargeOrderQuery;
import com.mrd.ad.business.wallet.dto.RefundOrderQuery;
import com.mrd.ad.business.wallet.dto.RefundReconcileActionRequest;
import com.mrd.ad.business.wallet.dto.WalletAccountQuery;
import com.mrd.ad.business.wallet.dto.WalletTransactionQuery;
import com.mrd.ad.business.wallet.mapper.AdBillingRuleMapper;
import com.mrd.ad.business.wallet.mapper.AdPayCallbackLogMapper;
import com.mrd.ad.business.wallet.mapper.AdPlayChargeMapper;
import com.mrd.ad.business.wallet.mapper.AdRechargeOrderMapper;
import com.mrd.ad.business.wallet.mapper.AdRefundOrderMapper;
import com.mrd.ad.business.wallet.mapper.AdRefundReconcileActionMapper;
import com.mrd.ad.business.wallet.mapper.AdWalletAccountMapper;
import com.mrd.ad.business.wallet.mapper.AdWalletTransactionMapper;
import com.mrd.ad.business.wallet.pay.WechatPayHelper;
import com.mrd.ad.business.wallet.service.AdWalletService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AdWalletServiceImpl implements AdWalletService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);

    private final AdWalletAccountMapper accountMapper;
    private final AdRechargeOrderMapper rechargeOrderMapper;
    private final AdRefundOrderMapper refundOrderMapper;
    private final AdRefundReconcileActionMapper refundReconcileActionMapper;
    private final AdWalletTransactionMapper transactionMapper;
    private final AdBillingRuleMapper billingRuleMapper;
    private final AdPlayChargeMapper playChargeMapper;
    private final AdPayCallbackLogMapper callbackLogMapper;
    private final AdOrderMapper adOrderMapper;
    private final AdMaterialMapper adMaterialMapper;
    private final AdAdvertiserMapper advertiserMapper;
    private final AdAgentMapper agentMapper;
    private final SysDataScopeService dataScopeService;
    private final AdPayProperties payProperties;
    private final RefundSyncService refundSyncService;

    public AdWalletServiceImpl(AdWalletAccountMapper accountMapper,
                               AdRechargeOrderMapper rechargeOrderMapper,
                               AdRefundOrderMapper refundOrderMapper,
                               AdRefundReconcileActionMapper refundReconcileActionMapper,
                               AdWalletTransactionMapper transactionMapper,
                               AdBillingRuleMapper billingRuleMapper,
                               AdPlayChargeMapper playChargeMapper,
                               AdPayCallbackLogMapper callbackLogMapper,
                               AdOrderMapper adOrderMapper,
                               AdMaterialMapper adMaterialMapper,
                               AdAdvertiserMapper advertiserMapper,
                               AdAgentMapper agentMapper,
                               SysDataScopeService dataScopeService,
                               AdPayProperties payProperties,
                               RefundSyncService refundSyncService) {
        this.accountMapper = accountMapper;
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.refundOrderMapper = refundOrderMapper;
        this.refundReconcileActionMapper = refundReconcileActionMapper;
        this.transactionMapper = transactionMapper;
        this.billingRuleMapper = billingRuleMapper;
        this.playChargeMapper = playChargeMapper;
        this.callbackLogMapper = callbackLogMapper;
        this.adOrderMapper = adOrderMapper;
        this.adMaterialMapper = adMaterialMapper;
        this.advertiserMapper = advertiserMapper;
        this.agentMapper = agentMapper;
        this.dataScopeService = dataScopeService;
        this.payProperties = payProperties;
        this.refundSyncService = refundSyncService;
    }

    @Override
    public PageResult<AdWalletAccount> pageAccounts(WalletAccountQuery query) {
        LambdaQueryWrapper<AdWalletAccount> wrapper = new LambdaQueryWrapper<AdWalletAccount>()
                .eq(AdWalletAccount::getDeleted, 0)
                .orderByDesc(AdWalletAccount::getCreateTime);
        applyOwnerScope(wrapper);
        if (StringUtils.isNotBlank(query.getOwnerType())) {
            wrapper.eq(AdWalletAccount::getOwnerType, query.getOwnerType());
        }
        if (query.getOwnerId() != null) {
            wrapper.eq(AdWalletAccount::getOwnerId, query.getOwnerId());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdWalletAccount::getStatus, query.getStatus());
        }
        Page<AdWalletAccount> page = accountMapper.selectPage(new Page<AdWalletAccount>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdWalletAccount>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdWalletAccount ensureAccount(String ownerType, Long ownerId) {
        validateOwner(ownerType, ownerId);
        AdWalletAccount existing = accountMapper.selectOne(new LambdaQueryWrapper<AdWalletAccount>()
                .eq(AdWalletAccount::getOwnerType, ownerType)
                .eq(AdWalletAccount::getOwnerId, ownerId)
                .eq(AdWalletAccount::getDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            if (dataScopeService.currentUser() != null) {
                dataScopeService.assertAdvertiserAgentVisible("advertiser".equals(ownerType) ? ownerId : null, "agent".equals(ownerType) ? ownerId : null);
            }
            return existing;
        }
        Date now = new Date();
        AdWalletAccount account = new AdWalletAccount();
        account.setAccountNo(nextNo("WA"));
        account.setOwnerType(ownerType);
        account.setOwnerId(ownerId);
        account.setAvailableBalance(ZERO);
        account.setFrozenBalance(ZERO);
        account.setTotalRecharge(ZERO);
        account.setTotalConsumed(ZERO);
        account.setCreditLimit(ZERO);
        account.setStatus("active");
        account.setCreateBy(dataScopeService.currentUserId());
        account.setCreateTime(now);
        account.setUpdateTime(now);
        account.setDeleted(0);
        accountMapper.insert(account);
        return account;
    }

    @Override
    public PageResult<AdRechargeOrder> pageRechargeOrders(RechargeOrderQuery query) {
        LambdaQueryWrapper<AdRechargeOrder> wrapper = new LambdaQueryWrapper<AdRechargeOrder>()
                .eq(AdRechargeOrder::getDeleted, 0)
                .orderByDesc(AdRechargeOrder::getCreateTime);
        applyRechargeScope(wrapper);
        if (StringUtils.isNotBlank(query.getOwnerType())) {
            wrapper.eq(AdRechargeOrder::getOwnerType, query.getOwnerType());
        }
        if (query.getOwnerId() != null) {
            wrapper.eq(AdRechargeOrder::getOwnerId, query.getOwnerId());
        }
        if (StringUtils.isNotBlank(query.getPayMethod())) {
            wrapper.eq(AdRechargeOrder::getPayMethod, query.getPayMethod());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdRechargeOrder::getStatus, query.getStatus());
        }
        if (StringUtils.isNotBlank(query.getRechargeNo())) {
            wrapper.eq(AdRechargeOrder::getRechargeNo, query.getRechargeNo());
        }
        Page<AdRechargeOrder> page = rechargeOrderMapper.selectPage(new Page<AdRechargeOrder>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdRechargeOrder>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }


    @Override
    public PageResult<AdPayCallbackLog> pagePayCallbackLogs(PayCallbackLogQuery query) {
        LambdaQueryWrapper<AdPayCallbackLog> wrapper = new LambdaQueryWrapper<AdPayCallbackLog>()
                .orderByDesc(AdPayCallbackLog::getCreateTime);
        if (StringUtils.isNotBlank(query.getPayMethod())) {
            wrapper.eq(AdPayCallbackLog::getPayMethod, query.getPayMethod());
        }
        if (StringUtils.isNotBlank(query.getRechargeNo())) {
            wrapper.like(AdPayCallbackLog::getRechargeNo, query.getRechargeNo());
        }
        if (StringUtils.isNotBlank(query.getTransactionNo())) {
            wrapper.like(AdPayCallbackLog::getTransactionNo, query.getTransactionNo());
        }
        if (StringUtils.isNotBlank(query.getCallbackStatus())) {
            wrapper.eq(AdPayCallbackLog::getCallbackStatus, query.getCallbackStatus());
        }
        Page<AdPayCallbackLog> page = callbackLogMapper.selectPage(new Page<AdPayCallbackLog>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdPayCallbackLog>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<AdRefundOrder> pageRefundOrders(RefundOrderQuery query) {
        LambdaQueryWrapper<AdRefundOrder> wrapper = new LambdaQueryWrapper<AdRefundOrder>()
                .eq(AdRefundOrder::getDeleted, 0)
                .orderByDesc(AdRefundOrder::getCreateTime);
        applyRefundScope(wrapper);
        if (StringUtils.isNotBlank(query.getOwnerType())) {
            wrapper.eq(AdRefundOrder::getOwnerType, query.getOwnerType());
        }
        if (query.getOwnerId() != null) {
            wrapper.eq(AdRefundOrder::getOwnerId, query.getOwnerId());
        }
        if (StringUtils.isNotBlank(query.getPayMethod())) {
            wrapper.eq(AdRefundOrder::getPayMethod, query.getPayMethod());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdRefundOrder::getStatus, query.getStatus());
        }
        if (StringUtils.isNotBlank(query.getRefundNo())) {
            wrapper.like(AdRefundOrder::getRefundNo, query.getRefundNo());
        }
        if (StringUtils.isNotBlank(query.getRechargeNo())) {
            wrapper.like(AdRefundOrder::getRechargeNo, query.getRechargeNo());
        }
        if (StringUtils.isNotBlank(query.getReconcileStatus())) {
            wrapper.eq(AdRefundOrder::getReconcileStatus, query.getReconcileStatus());
        }
        Page<AdRefundOrder> page = refundOrderMapper.selectPage(new Page<AdRefundOrder>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdRefundOrder>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdRefundOrder createRefundOrder(CreateRefundOrderRequest request) {
        AdRechargeOrder recharge = rechargeOrderMapper.selectOne(new LambdaQueryWrapper<AdRechargeOrder>()
                .eq(AdRechargeOrder::getId, request.getRechargeOrderId())
                .eq(AdRechargeOrder::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (recharge == null) {
            throw new BusinessException(404, "充值订单不存在");
        }
        dataScopeService.assertAdvertiserAgentVisible("advertiser".equals(recharge.getOwnerType()) ? recharge.getOwnerId() : null,
                "agent".equals(recharge.getOwnerType()) ? recharge.getOwnerId() : null);
        if (!"paid".equals(recharge.getStatus()) && !"confirmed".equals(recharge.getStatus())) {
            throw new BusinessException("只有已入账的充值订单可以申请退款");
        }
        BigDecimal amount = money(request.getRefundAmount());
        BigDecimal remaining = refundableAmount(recharge);
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException("退款金额不能超过可退金额：" + remaining.toPlainString());
        }
        Date now = new Date();
        AdRefundOrder refund = new AdRefundOrder();
        refund.setRefundNo(nextNo("RF"));
        refund.setRechargeOrderId(recharge.getId());
        refund.setRechargeNo(recharge.getRechargeNo());
        refund.setAccountId(recharge.getAccountId());
        refund.setOwnerType(recharge.getOwnerType());
        refund.setOwnerId(recharge.getOwnerId());
        refund.setPayMethod(recharge.getPayMethod());
        refund.setRefundAmount(amount);
        refund.setStatus("auditing");
        refund.setReservedAmount(ZERO);
        refund.setGatewayRefundStatus("NOT_SUBMITTED");
        refund.setReconcileStatus("pending");
        refund.setSyncRetryCount(0);
        refund.setRefundReason(StringUtils.trimToNull(request.getRefundReason()));
        refund.setTransactionNo(recharge.getTransactionNo());
        refund.setCreateBy(dataScopeService.currentUserId());
        refund.setCreateTime(now);
        refund.setUpdateTime(now);
        refund.setDeleted(0);
        refundOrderMapper.insert(refund);
        return refund;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdRefundOrder approveRefundOrder(Long id, AuditRefundOrderRequest request) {
        AdRefundOrder refund = lockedRefund(id);
        if (!"auditing".equals(refund.getStatus()) && !"failed".equals(refund.getStatus())) {
            throw new BusinessException("只有待审核或退款失败的申请可以审核通过");
        }
        dataScopeService.assertAdvertiserAgentVisible("advertiser".equals(refund.getOwnerType()) ? refund.getOwnerId() : null,
                "agent".equals(refund.getOwnerType()) ? refund.getOwnerId() : null);
        AdWalletAccount account = accountMapper.selectOne(new LambdaQueryWrapper<AdWalletAccount>()
                .eq(AdWalletAccount::getId, refund.getAccountId())
                .eq(AdWalletAccount::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account == null) {
            throw new BusinessException("资金账户不存在");
        }
        BigDecimal amount = money(refund.getRefundAmount());
        AdRechargeOrder recharge = rechargeOrderMapper.selectById(refund.getRechargeOrderId());
        if (recharge == null || amount.compareTo(refundableAmountExcluding(recharge, refund.getId())) > 0) {
            throw new BusinessException("退款金额超过原充值单当前剩余可退金额");
        }
        BigDecimal availableBefore = safe(account.getAvailableBalance());
        if (availableBefore.compareTo(amount) < 0) {
            throw new BusinessException("账户余额不足，无法退款");
        }
        if (money(refund.getReservedAmount()).compareTo(ZERO) > 0) {
            throw new BusinessException("退款单仍存在冻结金额，请先同步退款状态");
        }

        Date now = new Date();
        account.setAvailableBalance(availableBefore.subtract(amount));
        account.setFrozenBalance(safe(account.getFrozenBalance()).add(amount));
        account.setUpdateBy(dataScopeService.currentUserId());
        account.setUpdateTime(now);
        accountMapper.updateById(account);

        refund.setStatus("processing");
        refund.setReservedAmount(amount);
        refund.setAuditUserId(dataScopeService.currentUserId());
        refund.setAuditTime(now);
        refund.setAuditRemark(StringUtils.trimToNull(request == null ? null : request.getAuditRemark()));
        refund.setGatewayRefundStatus("SUBMITTING");
        refund.setReconcileStatus("pending");
        refund.setReconcileMessage("退款请求提交中");
        refund.setFailReason(null);
        refund.setSyncRetryCount(0);
        refund.setUpdateBy(dataScopeService.currentUserId());
        refund.setUpdateTime(now);
        refundOrderMapper.updateById(refund);

        RefundGatewayResult gatewayResult = requestRefundFromGateway(refund, request);
        if (StringUtils.isNotBlank(gatewayResult.getRefundTransactionNo())) {
            refund.setRefundTransactionNo(gatewayResult.getRefundTransactionNo());
        }
        refund.setGatewayRefundStatus(gatewayResult.isSuccess() ? "ACCEPTED" : "SUBMIT_FAILED");
        refund.setReconcileMessage(StringUtils.abbreviate(gatewayResult.getMessage(), 500));
        refund.setUpdateTime(new Date());
        refundOrderMapper.updateById(refund);
        if (!gatewayResult.isSuccess()) {
            return refundSyncService.markSubmissionFailed(refund.getId(), refund.getGatewayRefundStatus(),
                    gatewayResult.getMessage(), gatewayResult.isUncertain());
        }
        return refundSyncService.syncRefundOrder(refund.getId());
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdRefundOrder rejectRefundOrder(Long id, AuditRefundOrderRequest request) {
        AdRefundOrder refund = lockedRefund(id);
        if (!"auditing".equals(refund.getStatus())) {
            throw new BusinessException("只有待审核的退款申请可以驳回");
        }
        dataScopeService.assertAdvertiserAgentVisible("advertiser".equals(refund.getOwnerType()) ? refund.getOwnerId() : null,
                "agent".equals(refund.getOwnerType()) ? refund.getOwnerId() : null);
        Date now = new Date();
        refund.setStatus("rejected");
        refund.setAuditUserId(dataScopeService.currentUserId());
        refund.setAuditTime(now);
        refund.setAuditRemark(StringUtils.trimToNull(request == null ? null : request.getAuditRemark()));
        refund.setUpdateBy(dataScopeService.currentUserId());
        refund.setUpdateTime(now);
        refundOrderMapper.updateById(refund);
        return refund;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdRefundOrder syncRefundOrder(Long id) {
        AdRefundOrder refund = refundOrderMapper.selectById(id);
        if (refund == null || Integer.valueOf(1).equals(refund.getDeleted())) {
            throw new BusinessException(404, "退款申请不存在");
        }
        assertRefundVisible(refund);
        AdWalletAccount accountBefore = accountMapper.selectById(refund.getAccountId());
        AdRefundReconcileAction action = startReconcileAction(
                refund, accountBefore, "sync", "财务手动同步支付平台退款状态");
        AdRefundOrder result = refundSyncService.syncRefundOrder(id);
        AdWalletAccount accountAfter = accountMapper.selectById(result.getAccountId());
        finishReconcileAction(action, result, accountAfter, "success",
                StringUtils.defaultIfBlank(result.getReconcileMessage(), "退款状态同步完成"));
        return result;
    }

    @Override
    public List<AdRefundReconcileAction> listRefundReconcileActions(Long refundOrderId) {
        AdRefundOrder refund = refundOrderMapper.selectById(refundOrderId);
        if (refund == null || Integer.valueOf(1).equals(refund.getDeleted())) {
            throw new BusinessException(404, "退款申请不存在");
        }
        assertRefundVisible(refund);
        return refundReconcileActionMapper.selectList(new LambdaQueryWrapper<AdRefundReconcileAction>()
                .eq(AdRefundReconcileAction::getRefundOrderId, refundOrderId)
                .eq(AdRefundReconcileAction::getDeleted, 0)
                .orderByDesc(AdRefundReconcileAction::getCreateTime)
                .orderByDesc(AdRefundReconcileAction::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdRefundOrder handleRefundReconcileAction(Long id, RefundReconcileActionRequest request) {
        AdRefundOrder refund = lockedRefund(id);
        assertRefundVisible(refund);
        if (!"exception".equals(refund.getReconcileStatus())) {
            throw new BusinessException("只有对账异常的退款单可以人工处理");
        }
        String actionType = StringUtils.trimToEmpty(request.getActionType());
        String reason = StringUtils.trimToNull(request.getReason());
        if (reason == null) {
            throw new BusinessException("处理原因不能为空");
        }
        if ("confirm_refunded".equals(actionType)) {
            return confirmGatewayRefunded(refund, request, reason);
        }
        if ("release_reservation".equals(actionType)) {
            return confirmGatewayFailedAndRelease(refund, reason);
        }
        throw new BusinessException("不支持的退款异常处理类型");
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdRechargeOrder createRechargeOrder(CreateRechargeOrderRequest request) {
        AdWalletAccount account = ensureAccount(request.getOwnerType(), request.getOwnerId());
        Date now = new Date();
        AdRechargeOrder order = new AdRechargeOrder();
        order.setRechargeNo(nextNo("RC"));
        order.setAccountId(account.getId());
        order.setOwnerType(account.getOwnerType());
        order.setOwnerId(account.getOwnerId());
        order.setPayMethod(StringUtils.trim(request.getPayMethod()));
        order.setAmount(money(request.getAmount()));
        order.setStatus("bank_transfer".equals(request.getPayMethod()) ? "auditing" : "pending");
        order.setTransactionNo(StringUtils.trimToNull(request.getTransactionNo()));
        order.setVoucherNo(StringUtils.trimToNull(request.getVoucherNo()));
        order.setVoucherUrl(StringUtils.trimToNull(request.getVoucherUrl()));
        order.setRemark(StringUtils.trimToNull(request.getRemark()));
        order.setCreateBy(dataScopeService.currentUserId());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setDeleted(0);
        rechargeOrderMapper.insert(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RechargePaymentResult createRechargePayment(CreateRechargeOrderRequest request, String clientIp) {
        String payMethod = StringUtils.trimToEmpty(request.getPayMethod());
        if (!"wechat".equals(payMethod) && !"alipay".equals(payMethod)) {
            throw new BusinessException("扫码充值方式必须为 wechat 或 alipay");
        }
        AdRechargeOrder order = createRechargeOrder(request);
        RechargePaymentResult result;
        if ("wechat".equals(payMethod)) {
            result = createWechatPayment(order, clientIp);
        } else {
            result = createAlipayPayment(order);
        }
        order.setPrepayId(result.getPrepayId());
        order.setQrCodeUrl(result.getQrCodeUrl());
        order.setPayMessage(result.getMessage());
        order.setUpdateBy(dataScopeService.currentUserId());
        order.setUpdateTime(new Date());
        rechargeOrderMapper.updateById(order);
        result.setOrder(order);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdRechargeOrder confirmRechargeOrder(Long id, ConfirmRechargeRequest request) {
        AdRechargeOrder order = rechargeOrderMapper.selectById(id);
        if (order == null || Integer.valueOf(1).equals(order.getDeleted())) {
            throw new BusinessException(404, "充值订单不存在");
        }
        dataScopeService.assertAdvertiserAgentVisible("advertiser".equals(order.getOwnerType()) ? order.getOwnerId() : null,
                "agent".equals(order.getOwnerType()) ? order.getOwnerId() : null);
        if ("confirmed".equals(order.getStatus()) || "paid".equals(order.getStatus())) {
            throw new BusinessException("充值订单已入账");
        }
        return creditRechargeOrder(order, StringUtils.defaultIfBlank(request.getTransactionNo(), order.getTransactionNo()),
                StringUtils.defaultIfBlank(request.getRemark(), order.getRemark()), money(order.getAmount()), dataScopeService.currentUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdRechargeOrder markRechargePaid(String rechargeNo, String payMethod, String transactionNo, BigDecimal paidAmount, String payMessage) {
        return markRechargePaidWithResult(rechargeNo, payMethod, transactionNo, paidAmount, payMessage).getOrder();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleWechatNotify(String notifyXml) {
        AdPayCallbackLog log = createCallbackLog("wechat", notifyXml);
        String response;
        try {
            Map<String, String> params = WechatPayHelper.parseXml(notifyXml);
            log.setRechargeNo(params.get("out_trade_no"));
            log.setTransactionNo(params.get("transaction_id"));
            log.setTradeStatus(params.get("result_code"));
            if (StringUtils.isNotBlank(params.get("total_fee"))) {
                log.setPaidAmount(new BigDecimal(params.get("total_fee")).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
            }
            if (!"SUCCESS".equals(params.get("return_code")) || !"SUCCESS".equals(params.get("result_code"))) {
                response = wechatNotifyResult(false, StringUtils.defaultIfBlank(params.get("return_msg"), params.get("err_code_des")));
                finishCallbackLog(log, "failed", response, StringUtils.defaultIfBlank(params.get("return_msg"), params.get("err_code_des")));
                return response;
            }
            AdPayProperties.Wechat config = payProperties.getWechat();
            if (!WechatPayHelper.verify(params, config.getApiKey())) {
                response = wechatNotifyResult(false, "SIGN_ERROR");
                finishCallbackLog(log, "failed", response, "微信回调签名校验失败");
                return response;
            }
            PaidMarkResult result = markRechargePaidWithResult(params.get("out_trade_no"), "wechat", params.get("transaction_id"), log.getPaidAmount(), "微信扫码支付回调");
            if (result.getOrder() != null) {
                log.setRechargeOrderId(result.getOrder().getId());
            }
            response = wechatNotifyResult(true, "OK");
            finishCallbackLog(log, result.isDuplicate() ? "duplicate" : "success", response, null);
            return response;
        } catch (Exception ex) {
            response = wechatNotifyResult(false, "FAIL");
            finishCallbackLog(log, "failed", response, ex.getMessage());
            return response;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayNotify(Map<String, String> params) {
        AdPayCallbackLog log = createCallbackLog("alipay", JSON.toJSONString(params));
        String response;
        try {
            log.setRechargeNo(params.get("out_trade_no"));
            log.setTransactionNo(params.get("trade_no"));
            log.setTradeStatus(params.get("trade_status"));
            if (StringUtils.isNotBlank(params.get("total_amount"))) {
                log.setPaidAmount(new BigDecimal(params.get("total_amount")));
            }
            AdPayProperties.Alipay config = payProperties.getAlipay();
            boolean verified = AlipaySignature.rsaCheckV1(params, config.getAlipayPublicKey(), config.getCharset(), config.getSignType());
            if (!verified) {
                response = "failure";
                finishCallbackLog(log, "failed", response, "支付宝回调签名校验失败");
                return response;
            }
            String tradeStatus = params.get("trade_status");
            if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
                response = "success";
                finishCallbackLog(log, "ignored", response, "支付宝交易状态未完成：" + tradeStatus);
                return response;
            }
            PaidMarkResult result = markRechargePaidWithResult(params.get("out_trade_no"), "alipay", params.get("trade_no"), log.getPaidAmount(), "支付宝扫码支付回调");
            if (result.getOrder() != null) {
                log.setRechargeOrderId(result.getOrder().getId());
            }
            response = "success";
            finishCallbackLog(log, result.isDuplicate() ? "duplicate" : "success", response, null);
            return response;
        } catch (Exception ex) {
            response = "failure";
            finishCallbackLog(log, "failed", response, ex.getMessage());
            return response;
        }
    }

    private PaidMarkResult markRechargePaidWithResult(String rechargeNo, String payMethod, String transactionNo, BigDecimal paidAmount, String payMessage) {
        AdRechargeOrder order = rechargeOrderMapper.selectOne(new LambdaQueryWrapper<AdRechargeOrder>()
                .eq(AdRechargeOrder::getRechargeNo, rechargeNo)
                .eq(AdRechargeOrder::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (order == null) {
            throw new BusinessException(404, "充值订单不存在");
        }
        if (!StringUtils.equals(order.getPayMethod(), payMethod)) {
            throw new BusinessException("充值方式不匹配");
        }
        if ("confirmed".equals(order.getStatus()) || "paid".equals(order.getStatus())) {
            return new PaidMarkResult(order, true);
        }
        BigDecimal payAmount = money(paidAmount == null ? order.getAmount() : paidAmount);
        if (payAmount.compareTo(money(order.getAmount())) != 0) {
            throw new BusinessException("支付金额与充值金额不一致");
        }
        return new PaidMarkResult(creditRechargeOrder(order, transactionNo, payMessage, payAmount, 1L), false);
    }

    private AdPayCallbackLog createCallbackLog(String payMethod, String rawPayload) {
        AdPayCallbackLog log = new AdPayCallbackLog();
        log.setPayMethod(payMethod);
        log.setNotifyType("payment");
        log.setCallbackStatus("processing");
        log.setRawPayload(rawPayload);
        log.setCreateTime(new Date());
        callbackLogMapper.insert(log);
        return log;
    }

    private void finishCallbackLog(AdPayCallbackLog log, String status, String responseBody, String failReason) {
        if (log == null || log.getId() == null) {
            return;
        }
        log.setCallbackStatus(status);
        log.setResponseBody(StringUtils.abbreviate(responseBody, 1000));
        log.setFailReason(StringUtils.abbreviate(failReason, 500));
        log.setProcessedTime(new Date());
        callbackLogMapper.updateById(log);
    }
    private AdRechargeOrder creditRechargeOrder(AdRechargeOrder order, String transactionNo, String remark, BigDecimal paidAmount, Long auditUserId) {
        AdWalletAccount account = accountMapper.selectById(order.getAccountId());
        if (account == null || Integer.valueOf(1).equals(account.getDeleted())) {
            throw new BusinessException("资金账户不存在");
        }
        BigDecimal before = safe(account.getAvailableBalance());
        BigDecimal after = before.add(money(order.getAmount()));
        account.setAvailableBalance(after);
        account.setTotalRecharge(safe(account.getTotalRecharge()).add(money(order.getAmount())));
        account.setUpdateBy(dataScopeService.currentUserId());
        account.setUpdateTime(new Date());
        accountMapper.updateById(account);

        order.setStatus("paid");
        order.setTransactionNo(StringUtils.defaultIfBlank(transactionNo, order.getTransactionNo()));
        order.setPaidAmount(money(paidAmount));
        order.setPayMessage(StringUtils.defaultIfBlank(remark, order.getPayMessage()));
        order.setRemark(StringUtils.defaultIfBlank(remark, order.getRemark()));
        order.setAuditUserId(auditUserId);
        order.setAuditTime(new Date());
        order.setPaidTime(new Date());
        order.setUpdateBy(auditUserId);
        order.setUpdateTime(new Date());
        rechargeOrderMapper.updateById(order);

        insertTransaction(account, "recharge", "in", money(order.getAmount()), before, after,
                safe(account.getFrozenBalance()), safe(account.getFrozenBalance()), "recharge_order", order.getId(), "充值入账：" + order.getRechargeNo());
        return order;
    }

    @Override
    public PageResult<AdWalletTransaction> pageTransactions(WalletTransactionQuery query) {
        LambdaQueryWrapper<AdWalletTransaction> wrapper = new LambdaQueryWrapper<AdWalletTransaction>()
                .orderByDesc(AdWalletTransaction::getCreateTime);
        applyTransactionScope(wrapper);
        if (query.getAccountId() != null) {
            wrapper.eq(AdWalletTransaction::getAccountId, query.getAccountId());
        }
        if (StringUtils.isNotBlank(query.getOwnerType())) {
            wrapper.eq(AdWalletTransaction::getOwnerType, query.getOwnerType());
        }
        if (query.getOwnerId() != null) {
            wrapper.eq(AdWalletTransaction::getOwnerId, query.getOwnerId());
        }
        if (StringUtils.isNotBlank(query.getTransactionType())) {
            wrapper.eq(AdWalletTransaction::getTransactionType, query.getTransactionType());
        }
        Page<AdWalletTransaction> page = transactionMapper.selectPage(new Page<AdWalletTransaction>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdWalletTransaction>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<AdBillingRule> pageBillingRules(BillingRuleQuery query) {
        LambdaQueryWrapper<AdBillingRule> wrapper = new LambdaQueryWrapper<AdBillingRule>()
                .eq(AdBillingRule::getDeleted, 0)
                .orderByAsc(AdBillingRule::getPriority)
                .orderByDesc(AdBillingRule::getCreateTime);
        if (StringUtils.isNotBlank(query.getMaterialType())) {
            wrapper.eq(AdBillingRule::getMaterialType, query.getMaterialType());
        }
        if (StringUtils.isNotBlank(query.getChargeMode())) {
            wrapper.eq(AdBillingRule::getChargeMode, query.getChargeMode());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdBillingRule::getStatus, query.getStatus());
        }
        Page<AdBillingRule> page = billingRuleMapper.selectPage(new Page<AdBillingRule>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdBillingRule>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdBillingRule createBillingRule(BillingRuleSaveRequest request) {
        Date now = new Date();
        AdBillingRule rule = new AdBillingRule();
        rule.setRuleCode(nextNo("BR"));
        copyRule(request, rule);
        rule.setCreateBy(dataScopeService.currentUserId());
        rule.setCreateTime(now);
        rule.setUpdateTime(now);
        rule.setDeleted(0);
        billingRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdBillingRule updateBillingRule(Long id, BillingRuleSaveRequest request) {
        AdBillingRule rule = billingRuleMapper.selectById(id);
        if (rule == null || Integer.valueOf(1).equals(rule.getDeleted())) {
            throw new BusinessException(404, "计费规则不存在");
        }
        copyRule(request, rule);
        rule.setUpdateBy(dataScopeService.currentUserId());
        rule.setUpdateTime(new Date());
        billingRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    public PageResult<AdPlayCharge> pagePlayCharges(PlayChargeQuery query) {
        LambdaQueryWrapper<AdPlayCharge> wrapper = new LambdaQueryWrapper<AdPlayCharge>()
                .orderByDesc(AdPlayCharge::getCreateTime);
        applyChargeScope(wrapper);
        if (query.getAccountId() != null) {
            wrapper.eq(AdPlayCharge::getAccountId, query.getAccountId());
        }
        if (query.getAdId() != null) {
            wrapper.eq(AdPlayCharge::getAdId, query.getAdId());
        }
        if (query.getPlanId() != null) {
            wrapper.eq(AdPlayCharge::getPlanId, query.getPlanId());
        }
        if (StringUtils.isNotBlank(query.getOwnerType())) {
            wrapper.eq(AdPlayCharge::getOwnerType, query.getOwnerType());
        }
        if (query.getOwnerId() != null) {
            wrapper.eq(AdPlayCharge::getOwnerId, query.getOwnerId());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdPlayCharge::getStatus, query.getStatus());
        }
        Page<AdPlayCharge> page = playChargeMapper.selectPage(new Page<AdPlayCharge>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdPlayCharge>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdPlayCharge chargePlayLog(AdPlayLog playLog) {
        if (playLog == null || playLog.getId() == null) {
            return null;
        }
        AdPlayCharge existing = playChargeMapper.selectOne(new LambdaQueryWrapper<AdPlayCharge>()
                .eq(AdPlayCharge::getPlayLogId, playLog.getId())
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        if (StringUtils.isNotBlank(playLog.getPlayStatus()) && !"completed".equals(playLog.getPlayStatus())) {
            return insertSkippedCharge(playLog, "非播放完成状态，不扣费");
        }
        AdOrder ad = adOrderMapper.selectById(playLog.getAdId());
        if (ad == null || Integer.valueOf(1).equals(ad.getDeleted())) {
            return insertSkippedCharge(playLog, "广告不存在");
        }
        AdMaterial material = playLog.getMaterialId() == null ? null : adMaterialMapper.selectById(playLog.getMaterialId());
        AdBillingRule rule = findRule(material == null ? "all" : material.getMaterialType());
        if (rule == null) {
            return insertSkippedCharge(playLog, "未配置有效计费规则");
        }
        String ownerType = ad.getAgentId() == null ? "advertiser" : "agent";
        Long ownerId = ad.getAgentId() == null ? ad.getAdvertiserId() : ad.getAgentId();
        AdWalletAccount account = ensureAccount(ownerType, ownerId);
        BigDecimal quantity = quantity(playLog, rule);
        BigDecimal amount = money(rule.getUnitPrice().multiply(quantity));
        AdPlayCharge charge = baseCharge(playLog, ad, account);
        charge.setRuleId(rule.getId());
        charge.setChargeMode(rule.getChargeMode());
        charge.setUnitPrice(rule.getUnitPrice());
        charge.setQuantity(quantity);
        charge.setAmount(amount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            charge.setStatus("skipped");
            charge.setFailReason("计费金额为0");
            playChargeMapper.insert(charge);
            return charge;
        }
        BigDecimal before = safe(account.getAvailableBalance());
        BigDecimal credit = safe(account.getCreditLimit());
        if (before.add(credit).compareTo(amount) < 0) {
            charge.setStatus("failed");
            charge.setFailReason("账户余额不足");
            playChargeMapper.insert(charge);
            return charge;
        }
        BigDecimal after = before.subtract(amount);
        account.setAvailableBalance(after);
        account.setTotalConsumed(safe(account.getTotalConsumed()).add(amount));
        account.setUpdateTime(new Date());
        accountMapper.updateById(account);

        charge.setStatus("success");
        playChargeMapper.insert(charge);
        insertTransaction(account, "consume", "out", amount, before, after,
                safe(account.getFrozenBalance()), safe(account.getFrozenBalance()), "play_charge", charge.getId(), "播放扣费：" + charge.getChargeNo());
        return charge;
    }

    private RechargePaymentResult createWechatPayment(AdRechargeOrder order, String clientIp) {
        AdPayProperties.Wechat config = payProperties.getWechat();
        assertWechatConfig(config);
        Map<String, String> params = new HashMap<String, String>();
        params.put("appid", config.getAppId());
        params.put("mch_id", config.getMchId());
        params.put("nonce_str", WechatPayHelper.nonce());
        params.put("body", config.getBodyPrefix() + "-" + order.getRechargeNo());
        params.put("out_trade_no", order.getRechargeNo());
        params.put("total_fee", String.valueOf(WechatPayHelper.yuanToCent(order.getAmount())));
        params.put("spbill_create_ip", StringUtils.defaultIfBlank(clientIp, "127.0.0.1"));
        params.put("notify_url", config.getNotifyUrl());
        params.put("trade_type", "NATIVE");
        params.put("product_id", order.getRechargeNo());
        params.put("sign", WechatPayHelper.sign(params, config.getApiKey()));
        Map<String, String> response = WechatPayHelper.parseXml(WechatPayHelper.postXml(config.getUnifiedOrderUrl(), WechatPayHelper.toXml(params)));
        if (!"SUCCESS".equals(response.get("return_code")) || !"SUCCESS".equals(response.get("result_code"))) {
            throw new BusinessException("微信支付下单失败：" + StringUtils.defaultIfBlank(response.get("return_msg"), response.get("err_code_des")));
        }
        RechargePaymentResult result = new RechargePaymentResult();
        result.setPayMethod("wechat");
        result.setPrepayId(response.get("prepay_id"));
        result.setQrCodeUrl(response.get("code_url"));
        result.setMessage("微信扫码支付订单已生成");
        return result;
    }

    private RechargePaymentResult createAlipayPayment(AdRechargeOrder order) {
        AdPayProperties.Alipay config = payProperties.getAlipay();
        assertAlipayConfig(config);
        AlipayClient client = new DefaultAlipayClient(config.getGatewayUrl(), config.getAppId(), config.getMerchantPrivateKey(),
                "json", config.getCharset(), config.getAlipayPublicKey(), config.getSignType());
        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
        request.setNotifyUrl(config.getNotifyUrl());
        request.setBizContent("{"
                + "\"out_trade_no\":\"" + order.getRechargeNo() + "\","
                + "\"total_amount\":\"" + money(order.getAmount()).toPlainString() + "\","
                + "\"subject\":\"" + config.getSubjectPrefix() + "-" + order.getRechargeNo() + "\""
                + "}");
        try {
            AlipayTradePrecreateResponse response = client.execute(request);
            if (!response.isSuccess()) {
                throw new BusinessException("支付宝支付下单失败：" + StringUtils.defaultIfBlank(response.getSubMsg(), response.getMsg()));
            }
            RechargePaymentResult result = new RechargePaymentResult();
            result.setPayMethod("alipay");
            result.setPrepayId(response.getOutTradeNo());
            result.setQrCodeUrl(response.getQrCode());
            result.setMessage("支付宝扫码支付订单已生成");
            return result;
        } catch (AlipayApiException ex) {
            throw new BusinessException("支付宝支付请求失败：" + ex.getMessage());
        }
    }

    private void assertWechatConfig(AdPayProperties.Wechat config) {
        if (config == null || !config.isEnabled()
                || StringUtils.isAnyBlank(config.getAppId(), config.getMchId(), config.getApiKey(), config.getNotifyUrl(), config.getUnifiedOrderUrl())) {
            throw new BusinessException("微信支付通道未启用或配置不完整");
        }
    }

    private void assertAlipayConfig(AdPayProperties.Alipay config) {
        if (config == null || !config.isEnabled()
                || StringUtils.isAnyBlank(config.getAppId(), config.getMerchantPrivateKey(), config.getAlipayPublicKey(), config.getNotifyUrl(), config.getGatewayUrl())) {
            throw new BusinessException("支付宝支付通道未启用或配置不完整");
        }
    }

    private String wechatNotifyResult(boolean success, String message) {
        Map<String, String> result = new HashMap<String, String>();
        result.put("return_code", success ? "SUCCESS" : "FAIL");
        result.put("return_msg", StringUtils.defaultIfBlank(message, success ? "OK" : "FAIL"));
        return WechatPayHelper.toXml(result);
    }

    private RefundGatewayResult requestRefundFromGateway(AdRefundOrder refund, AuditRefundOrderRequest request) {
        String method = StringUtils.defaultString(refund.getPayMethod());
        if ("alipay".equals(method) && payProperties.getAlipay().isRefundEnabled()) {
            return requestAlipayRefund(refund);
        }
        if ("wechat".equals(method) && payProperties.getWechat().isRefundEnabled()) {
            return requestWechatRefund(refund);
        }
        return RefundGatewayResult.success(StringUtils.trimToNull(request == null ? null : request.getRefundTransactionNo()), "人工退款模式");
    }

    private RefundGatewayResult requestAlipayRefund(AdRefundOrder refund) {
        AdPayProperties.Alipay config = payProperties.getAlipay();
        assertAlipayConfig(config);
        AlipayClient client = new DefaultAlipayClient(config.getGatewayUrl(), config.getAppId(), config.getMerchantPrivateKey(),
                "json", config.getCharset(), config.getAlipayPublicKey(), config.getSignType());
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        Map<String, String> biz = new HashMap<String, String>();
        biz.put("out_trade_no", refund.getRechargeNo());
        if (StringUtils.isNotBlank(refund.getTransactionNo())) {
            biz.put("trade_no", refund.getTransactionNo());
        }
        biz.put("refund_amount", money(refund.getRefundAmount()).toPlainString());
        biz.put("out_request_no", refund.getRefundNo());
        request.setBizContent(JSON.toJSONString(biz));
        try {
            AlipayTradeRefundResponse response = client.execute(request);
            if (!response.isSuccess() || !"Y".equals(response.getFundChange())) {
                return RefundGatewayResult.failed("支付宝退款失败：" + StringUtils.defaultIfBlank(response.getSubMsg(), response.getMsg()));
            }
            return RefundGatewayResult.success(StringUtils.defaultIfBlank(response.getTradeNo(), refund.getTransactionNo()), "支付宝退款成功");
        } catch (AlipayApiException ex) {
            return RefundGatewayResult.uncertain("支付宝退款请求异常：" + ex.getMessage());
        }
    }

    private RefundGatewayResult requestWechatRefund(AdRefundOrder refund) {
        AdPayProperties.Wechat config = payProperties.getWechat();
        assertWechatConfig(config);
        if (StringUtils.isBlank(config.getRefundUrl())) {
            return RefundGatewayResult.failed("微信退款接口地址未配置");
        }
        if (StringUtils.isBlank(config.getRefundCertPath())) {
            return RefundGatewayResult.failed("微信退款商户证书未配置，请配置 MRD_PAY_WECHAT_REFUND_CERT_PATH");
        }
        AdRechargeOrder recharge = rechargeOrderMapper.selectById(refund.getRechargeOrderId());
        BigDecimal totalAmount = recharge == null ? refund.getRefundAmount() : (recharge.getPaidAmount() == null ? recharge.getAmount() : recharge.getPaidAmount());
        Map<String, String> params = new HashMap<String, String>();
        params.put("appid", config.getAppId());
        params.put("mch_id", config.getMchId());
        params.put("nonce_str", WechatPayHelper.nonce());
        if (StringUtils.isNotBlank(refund.getTransactionNo())) {
            params.put("transaction_id", refund.getTransactionNo());
        }
        params.put("out_trade_no", refund.getRechargeNo());
        params.put("out_refund_no", refund.getRefundNo());
        params.put("total_fee", String.valueOf(WechatPayHelper.yuanToCent(totalAmount)));
        params.put("refund_fee", String.valueOf(WechatPayHelper.yuanToCent(refund.getRefundAmount())));
        params.put("refund_desc", "ViiAD充值退款");
        params.put("sign", WechatPayHelper.sign(params, config.getApiKey()));
        try {
            String responseXml = WechatPayHelper.postXmlWithCert(config.getRefundUrl(), WechatPayHelper.toXml(params),
                    config.getRefundCertPath(), StringUtils.defaultIfBlank(config.getRefundCertPassword(), config.getMchId()));
            if (StringUtils.isBlank(responseXml) || !responseXml.trim().startsWith("<xml")) {
                return RefundGatewayResult.uncertain("微信退款失败：微信返回内容不是有效XML，请检查商户证书和退款接口配置");
            }
            Map<String, String> response = WechatPayHelper.parseXml(responseXml);
            if (!"SUCCESS".equals(response.get("return_code"))) {
                return RefundGatewayResult.uncertain("微信退款通信失败：" + response.get("return_msg"));
            }
            if (!"SUCCESS".equals(response.get("result_code"))) {
                String errorCode = response.get("err_code");
                String message = "微信退款失败：" + StringUtils.defaultIfBlank(response.get("err_code_des"), errorCode);
                return "SYSTEMERROR".equals(errorCode) ? RefundGatewayResult.uncertain(message) : RefundGatewayResult.failed(message);
            }
            return RefundGatewayResult.success(StringUtils.defaultIfBlank(response.get("refund_id"), refund.getRefundNo()), "微信退款成功");
        } catch (Exception ex) {
            return RefundGatewayResult.uncertain("微信退款请求异常：" + ex.getMessage());
        }
    }
    private void copyRule(BillingRuleSaveRequest request, AdBillingRule rule) {
        rule.setRuleName(StringUtils.trim(request.getRuleName()));
        rule.setMaterialType(StringUtils.defaultIfBlank(request.getMaterialType(), "all").trim());
        rule.setChargeMode(StringUtils.defaultIfBlank(request.getChargeMode(), "play_count").trim());
        rule.setUnitPrice(request.getUnitPrice());
        rule.setPriority(request.getPriority() == null ? 100 : request.getPriority());
        rule.setStatus(StringUtils.defaultIfBlank(request.getStatus(), "active"));
        rule.setRemark(StringUtils.trimToNull(request.getRemark()));
    }

    private AdBillingRule findRule(String materialType) {
        Date now = new Date();
        List<AdBillingRule> rules = billingRuleMapper.selectList(new LambdaQueryWrapper<AdBillingRule>()
                .eq(AdBillingRule::getDeleted, 0)
                .eq(AdBillingRule::getStatus, "active")
                .and(wrapper -> wrapper.eq(AdBillingRule::getMaterialType, materialType).or().eq(AdBillingRule::getMaterialType, "all"))
                .and(wrapper -> wrapper.isNull(AdBillingRule::getEffectiveStart).or().le(AdBillingRule::getEffectiveStart, now))
                .and(wrapper -> wrapper.isNull(AdBillingRule::getEffectiveEnd).or().ge(AdBillingRule::getEffectiveEnd, now))
                .orderByAsc(AdBillingRule::getPriority)
                .orderByDesc(AdBillingRule::getCreateTime));
        return rules == null || rules.isEmpty() ? null : rules.get(0);
    }

    private BigDecimal quantity(AdPlayLog playLog, AdBillingRule rule) {
        if ("duration_second".equals(rule.getChargeMode())) {
            return BigDecimal.valueOf(playLog.getPlayDuration() == null ? 0 : playLog.getPlayDuration());
        }
        return BigDecimal.valueOf(playLog.getPlayCount() == null ? 0 : playLog.getPlayCount());
    }

    private AdPlayCharge insertSkippedCharge(AdPlayLog playLog, String reason) {
        AdOrder ad = playLog.getAdId() == null ? null : adOrderMapper.selectById(playLog.getAdId());
        AdWalletAccount account = null;
        if (ad != null && ad.getAdvertiserId() != null) {
            String ownerType = ad.getAgentId() == null ? "advertiser" : "agent";
            Long ownerId = ad.getAgentId() == null ? ad.getAdvertiserId() : ad.getAgentId();
            account = ensureAccount(ownerType, ownerId);
        }
        AdPlayCharge charge = baseCharge(playLog, ad, account);
        charge.setQuantity(BigDecimal.ZERO);
        charge.setAmount(ZERO);
        charge.setStatus("skipped");
        charge.setFailReason(reason);
        playChargeMapper.insert(charge);
        return charge;
    }

    private AdPlayCharge baseCharge(AdPlayLog playLog, AdOrder ad, AdWalletAccount account) {
        AdPlayCharge charge = new AdPlayCharge();
        charge.setChargeNo(nextNo("PC"));
        charge.setPlayLogId(playLog.getId());
        charge.setAdId(playLog.getAdId());
        charge.setPlanId(playLog.getPlanId());
        charge.setMaterialId(playLog.getMaterialId());
        charge.setDeviceId(playLog.getDeviceId());
        charge.setAdvertiserId(ad == null ? null : ad.getAdvertiserId());
        charge.setAgentId(ad == null ? null : ad.getAgentId());
        charge.setAccountId(account == null ? null : account.getId());
        charge.setOwnerType(account == null ? null : account.getOwnerType());
        charge.setOwnerId(account == null ? null : account.getOwnerId());
        charge.setUnitPrice(BigDecimal.ZERO);
        charge.setQuantity(BigDecimal.ZERO);
        charge.setAmount(ZERO);
        charge.setCreateTime(new Date());
        return charge;
    }

    private void insertTransaction(AdWalletAccount account, String type, String direction, BigDecimal amount,
                                   BigDecimal balanceBefore, BigDecimal balanceAfter,
                                   BigDecimal frozenBefore, BigDecimal frozenAfter,
                                   String bizType, Long bizId, String remark) {
        AdWalletTransaction transaction = new AdWalletTransaction();
        transaction.setTransactionNo(nextNo("WT"));
        transaction.setAccountId(account.getId());
        transaction.setOwnerType(account.getOwnerType());
        transaction.setOwnerId(account.getOwnerId());
        transaction.setTransactionType(type);
        transaction.setDirection(direction);
        transaction.setAmount(money(amount));
        transaction.setBalanceBefore(money(balanceBefore));
        transaction.setBalanceAfter(money(balanceAfter));
        transaction.setFrozenBefore(money(frozenBefore));
        transaction.setFrozenAfter(money(frozenAfter));
        transaction.setBizType(bizType);
        transaction.setBizId(bizId);
        transaction.setRemark(remark);
        transaction.setCreateTime(new Date());
        transactionMapper.insert(transaction);
    }


    private AdRefundOrder confirmGatewayRefunded(AdRefundOrder refund,
                                                  RefundReconcileActionRequest request,
                                                  String reason) {
        if (Arrays.asList("auditing", "rejected", "cancelled").contains(refund.getStatus())) {
            throw new BusinessException("当前退款状态不能确认渠道已退款");
        }
        String refundTransactionNo = StringUtils.defaultIfBlank(
                StringUtils.trimToNull(request.getRefundTransactionNo()), refund.getRefundTransactionNo());
        if (StringUtils.isBlank(refundTransactionNo)) {
            throw new BusinessException("请填写支付平台退款流水号");
        }

        AdWalletAccount account = lockedWalletAccount(refund.getAccountId());
        AdRefundReconcileAction action = startReconcileAction(refund, account, "confirm_refunded", reason);
        BigDecimal amount = money(refund.getRefundAmount());
        BigDecimal availableBefore = money(account.getAvailableBalance());
        BigDecimal frozenBefore = money(account.getFrozenBalance());
        BigDecimal availableAfter = availableBefore;
        BigDecimal frozenAfter = frozenBefore;
        List<AdWalletTransaction> transactions = refundTransactions(refund.getId());

        if (transactions.size() > 1) {
            throw new BusinessException("存在重复退款资金流水，不能自动处理，请先核查流水");
        }
        if (transactions.size() == 1) {
            AdWalletTransaction transaction = transactions.get(0);
            if (money(transaction.getAmount()).compareTo(amount) != 0) {
                throw new BusinessException("退款流水金额与退款单金额不一致，不能自动处理");
            }
            if (money(refund.getReservedAmount()).compareTo(ZERO) != 0) {
                throw new BusinessException("已存在退款流水但冻结金额仍未释放，不能自动处理");
            }
        } else {
            BigDecimal reserved = money(refund.getReservedAmount());
            if (reserved.compareTo(ZERO) > 0) {
                if (reserved.compareTo(amount) != 0) {
                    throw new BusinessException("退款冻结金额与退款单金额不一致，不能自动处理");
                }
                if (frozenBefore.compareTo(reserved) < 0) {
                    throw new BusinessException("账户冻结余额不足，不能自动补记退款");
                }
                frozenAfter = frozenBefore.subtract(reserved);
            } else {
                if (availableBefore.compareTo(amount) < 0) {
                    throw new BusinessException("账户可用余额不足，不能自动补记退款");
                }
                availableAfter = availableBefore.subtract(amount);
            }
            account.setAvailableBalance(availableAfter);
            account.setFrozenBalance(frozenAfter);
            account.setTotalRecharge(money(account.getTotalRecharge()).subtract(amount).max(ZERO));
            account.setUpdateBy(dataScopeService.currentUserId());
            account.setUpdateTime(new Date());
            accountMapper.updateById(account);
            insertTransaction(account, "refund", "out", amount,
                    availableBefore, availableAfter, frozenBefore, frozenAfter,
                    "refund_order", refund.getId(), "退款异常处理补记：" + refund.getRefundNo());
        }

        Date now = new Date();
        refund.setStatus("refunded");
        refund.setReservedAmount(ZERO);
        refund.setRefundTransactionNo(refundTransactionNo);
        refund.setRefundTime(refund.getRefundTime() == null ? now : refund.getRefundTime());
        refund.setGatewayRefundStatus("MANUAL_CONFIRMED_SUCCESS");
        refund.setReconcileStatus("matched");
        refund.setReconcileMessage(StringUtils.abbreviate("财务确认渠道已退款：" + reason, 500));
        refund.setFailReason(null);
        refund.setUpdateBy(dataScopeService.currentUserId());
        refund.setUpdateTime(now);
        refundOrderMapper.updateById(refund);

        AdWalletAccount accountAfter = accountMapper.selectById(account.getId());
        finishReconcileAction(action, refund, accountAfter, "success", refund.getReconcileMessage());
        return refund;
    }

    private AdRefundOrder confirmGatewayFailedAndRelease(AdRefundOrder refund, String reason) {
        if ("refunded".equals(refund.getStatus())) {
            throw new BusinessException("本地已记录退款成功，不能直接释放冻结金额");
        }
        if (!refundTransactions(refund.getId()).isEmpty()) {
            throw new BusinessException("退款单已存在资金流水，不能释放冻结金额");
        }

        AdWalletAccount account = lockedWalletAccount(refund.getAccountId());
        AdRefundReconcileAction action = startReconcileAction(refund, account, "release_reservation", reason);
        BigDecimal reserved = money(refund.getReservedAmount());
        BigDecimal availableBefore = money(account.getAvailableBalance());
        BigDecimal frozenBefore = money(account.getFrozenBalance());
        BigDecimal availableAfter = availableBefore;
        BigDecimal frozenAfter = frozenBefore;
        if (reserved.compareTo(ZERO) > 0) {
            if (frozenBefore.compareTo(reserved) < 0) {
                throw new BusinessException("账户冻结余额不足，不能自动释放，请先核查账户流水");
            }
            availableAfter = availableBefore.add(reserved);
            frozenAfter = frozenBefore.subtract(reserved);
            account.setAvailableBalance(availableAfter);
            account.setFrozenBalance(frozenAfter);
            account.setUpdateBy(dataScopeService.currentUserId());
            account.setUpdateTime(new Date());
            accountMapper.updateById(account);
        }

        Date now = new Date();
        refund.setStatus("failed");
        refund.setReservedAmount(ZERO);
        refund.setGatewayRefundStatus("MANUAL_CONFIRMED_FAILED");
        refund.setReconcileStatus("matched");
        refund.setReconcileMessage(StringUtils.abbreviate("财务确认渠道退款失败并释放冻结：" + reason, 500));
        refund.setFailReason(StringUtils.abbreviate(reason, 500));
        refund.setUpdateBy(dataScopeService.currentUserId());
        refund.setUpdateTime(now);
        refundOrderMapper.updateById(refund);

        AdWalletAccount accountAfter = accountMapper.selectById(account.getId());
        finishReconcileAction(action, refund, accountAfter, "success", refund.getReconcileMessage());
        return refund;
    }

    private AdWalletAccount lockedWalletAccount(Long accountId) {
        AdWalletAccount account = accountMapper.selectOne(new LambdaQueryWrapper<AdWalletAccount>()
                .eq(AdWalletAccount::getId, accountId)
                .eq(AdWalletAccount::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account == null) {
            throw new BusinessException("资金账户不存在");
        }
        return account;
    }

    private List<AdWalletTransaction> refundTransactions(Long refundOrderId) {
        return transactionMapper.selectList(new LambdaQueryWrapper<AdWalletTransaction>()
                .eq(AdWalletTransaction::getBizType, "refund_order")
                .eq(AdWalletTransaction::getBizId, refundOrderId)
                .eq(AdWalletTransaction::getTransactionType, "refund"));
    }

    private AdRefundReconcileAction startReconcileAction(AdRefundOrder refund,
                                                          AdWalletAccount account,
                                                          String actionType,
                                                          String reason) {
        AdRefundReconcileAction action = new AdRefundReconcileAction();
        action.setRefundOrderId(refund.getId());
        action.setRefundNo(refund.getRefundNo());
        action.setActionType(actionType);
        action.setReason(StringUtils.abbreviate(reason, 500));
        action.setOperatorUserId(dataScopeService.currentUserId());
        SysUser operator = dataScopeService.currentUser();
        if (operator != null) {
            action.setOperatorName(StringUtils.defaultIfBlank(operator.getRealName(), operator.getUsername()));
        }
        action.setRefundStatusBefore(refund.getStatus());
        action.setReconcileStatusBefore(refund.getReconcileStatus());
        action.setReservedAmountBefore(money(refund.getReservedAmount()));
        if (account != null) {
            action.setAvailableBalanceBefore(money(account.getAvailableBalance()));
            action.setFrozenBalanceBefore(money(account.getFrozenBalance()));
        }
        action.setCreateBy(dataScopeService.currentUserId());
        action.setCreateTime(new Date());
        action.setDeleted(0);
        return action;
    }

    private void finishReconcileAction(AdRefundReconcileAction action,
                                       AdRefundOrder refund,
                                       AdWalletAccount account,
                                       String resultStatus,
                                       String resultMessage) {
        action.setGatewayRefundStatus(refund.getGatewayRefundStatus());
        action.setRefundStatusAfter(refund.getStatus());
        action.setReconcileStatusAfter(refund.getReconcileStatus());
        action.setReservedAmountAfter(money(refund.getReservedAmount()));
        if (account != null) {
            action.setAvailableBalanceAfter(money(account.getAvailableBalance()));
            action.setFrozenBalanceAfter(money(account.getFrozenBalance()));
        }
        action.setResultStatus(resultStatus);
        action.setResultMessage(StringUtils.abbreviate(resultMessage, 500));
        refundReconcileActionMapper.insert(action);
    }

    private void assertRefundVisible(AdRefundOrder refund) {
        if (dataScopeService.currentUser() == null) {
            return;
        }
        dataScopeService.assertAdvertiserAgentVisible(
                "advertiser".equals(refund.getOwnerType()) ? refund.getOwnerId() : null,
                "agent".equals(refund.getOwnerType()) ? refund.getOwnerId() : null);
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

    private BigDecimal refundableAmountExcluding(AdRechargeOrder recharge, Long excludedRefundId) {
        BigDecimal paid = money(recharge.getPaidAmount() == null ? recharge.getAmount() : recharge.getPaidAmount());
        LambdaQueryWrapper<AdRefundOrder> wrapper = new LambdaQueryWrapper<AdRefundOrder>()
                .eq(AdRefundOrder::getRechargeOrderId, recharge.getId())
                .eq(AdRefundOrder::getDeleted, 0)
                .in(AdRefundOrder::getStatus, Arrays.asList("auditing", "processing", "refunded", "approved"));
        if (excludedRefundId != null) {
            wrapper.ne(AdRefundOrder::getId, excludedRefundId);
        }
        List<AdRefundOrder> refunds = refundOrderMapper.selectList(wrapper);
        BigDecimal used = ZERO;
        if (refunds != null) {
            for (AdRefundOrder item : refunds) {
                used = used.add(money(item.getRefundAmount()));
            }
        }
        BigDecimal remaining = paid.subtract(used);
        return remaining.compareTo(ZERO) < 0 ? ZERO : remaining;
    }
    private BigDecimal refundableAmount(AdRechargeOrder recharge) {
        BigDecimal paid = money(recharge.getPaidAmount() == null ? recharge.getAmount() : recharge.getPaidAmount());
        List<AdRefundOrder> refunds = refundOrderMapper.selectList(new LambdaQueryWrapper<AdRefundOrder>()
                .eq(AdRefundOrder::getRechargeOrderId, recharge.getId())
                .eq(AdRefundOrder::getDeleted, 0)
                .in(AdRefundOrder::getStatus, Arrays.asList("auditing", "processing", "refunded", "approved")));
        BigDecimal used = ZERO;
        if (refunds != null) {
            for (AdRefundOrder item : refunds) {
                used = used.add(money(item.getRefundAmount()));
            }
        }
        BigDecimal remaining = paid.subtract(used);
        return remaining.compareTo(ZERO) < 0 ? ZERO : remaining;
    }
    private void validateOwner(String ownerType, Long ownerId) {
        if (!"advertiser".equals(ownerType) && !"agent".equals(ownerType)) {
            throw new BusinessException("账户主体类型必须为 advertiser 或 agent");
        }
        if ("advertiser".equals(ownerType)) {
            AdAdvertiser advertiser = advertiserMapper.selectById(ownerId);
            if (advertiser == null || Integer.valueOf(1).equals(advertiser.getDeleted())) {
                throw new BusinessException("广告主不存在");
            }
            if (dataScopeService.currentUser() != null) {
                dataScopeService.assertAdvertiserAgentVisible(ownerId, advertiser.getAgentId());
            }
            return;
        }
        AdAgent agent = agentMapper.selectById(ownerId);
        if (agent == null || Integer.valueOf(1).equals(agent.getDeleted())) {
            throw new BusinessException("代理商不存在");
        }
        if (dataScopeService.currentUser() != null) {
            dataScopeService.assertAdvertiserAgentVisible(null, ownerId);
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private BigDecimal money(BigDecimal value) {
        return safe(value).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private String nextNo(String prefix) {
        return prefix + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
    }

    private void applyOwnerScope(LambdaQueryWrapper<AdWalletAccount> wrapper) {
        if (dataScopeService.isAllData()) {
            return;
        }
        if (dataScopeService.isAdvertiserScope() && dataScopeService.currentAdvertiserId() != null) {
            wrapper.eq(AdWalletAccount::getOwnerType, "advertiser").eq(AdWalletAccount::getOwnerId, dataScopeService.currentAdvertiserId());
            return;
        }
        if (dataScopeService.isAgentScope() && dataScopeService.currentAgentId() != null) {
            wrapper.eq(AdWalletAccount::getOwnerType, "agent").eq(AdWalletAccount::getOwnerId, dataScopeService.currentAgentId());
            return;
        }
        wrapper.eq(AdWalletAccount::getId, -1L);
    }

    private void applyRechargeScope(LambdaQueryWrapper<AdRechargeOrder> wrapper) {
        if (dataScopeService.isAllData()) {
            return;
        }
        if (dataScopeService.isAdvertiserScope() && dataScopeService.currentAdvertiserId() != null) {
            wrapper.eq(AdRechargeOrder::getOwnerType, "advertiser").eq(AdRechargeOrder::getOwnerId, dataScopeService.currentAdvertiserId());
            return;
        }
        if (dataScopeService.isAgentScope() && dataScopeService.currentAgentId() != null) {
            wrapper.eq(AdRechargeOrder::getOwnerType, "agent").eq(AdRechargeOrder::getOwnerId, dataScopeService.currentAgentId());
            return;
        }
        wrapper.eq(AdRechargeOrder::getId, -1L);
    }


    private void applyRefundScope(LambdaQueryWrapper<AdRefundOrder> wrapper) {
        if (dataScopeService.isAllData()) {
            return;
        }
        if (dataScopeService.isAdvertiserScope() && dataScopeService.currentAdvertiserId() != null) {
            wrapper.eq(AdRefundOrder::getOwnerType, "advertiser").eq(AdRefundOrder::getOwnerId, dataScopeService.currentAdvertiserId());
            return;
        }
        if (dataScopeService.isAgentScope() && dataScopeService.currentAgentId() != null) {
            wrapper.eq(AdRefundOrder::getOwnerType, "agent").eq(AdRefundOrder::getOwnerId, dataScopeService.currentAgentId());
            return;
        }
        wrapper.eq(AdRefundOrder::getId, -1L);
    }    private void applyTransactionScope(LambdaQueryWrapper<AdWalletTransaction> wrapper) {
        if (dataScopeService.isAllData()) {
            return;
        }
        if (dataScopeService.isAdvertiserScope() && dataScopeService.currentAdvertiserId() != null) {
            wrapper.eq(AdWalletTransaction::getOwnerType, "advertiser").eq(AdWalletTransaction::getOwnerId, dataScopeService.currentAdvertiserId());
            return;
        }
        if (dataScopeService.isAgentScope() && dataScopeService.currentAgentId() != null) {
            wrapper.eq(AdWalletTransaction::getOwnerType, "agent").eq(AdWalletTransaction::getOwnerId, dataScopeService.currentAgentId());
            return;
        }
        wrapper.eq(AdWalletTransaction::getId, -1L);
    }

    private void applyChargeScope(LambdaQueryWrapper<AdPlayCharge> wrapper) {
        if (dataScopeService.isAllData()) {
            return;
        }
        if (dataScopeService.isAdvertiserScope() && dataScopeService.currentAdvertiserId() != null) {
            wrapper.eq(AdPlayCharge::getOwnerType, "advertiser").eq(AdPlayCharge::getOwnerId, dataScopeService.currentAdvertiserId());
            return;
        }
        if (dataScopeService.isAgentScope() && dataScopeService.currentAgentId() != null) {
            wrapper.eq(AdPlayCharge::getOwnerType, "agent").eq(AdPlayCharge::getOwnerId, dataScopeService.currentAgentId());
            return;
        }
        wrapper.eq(AdPlayCharge::getId, -1L);
    }
    private static class RefundGatewayResult {
        private final boolean success;
        private final boolean uncertain;
        private final String refundTransactionNo;
        private final String message;

        private RefundGatewayResult(boolean success, boolean uncertain, String refundTransactionNo, String message) {
            this.success = success;
            this.uncertain = uncertain;
            this.refundTransactionNo = refundTransactionNo;
            this.message = message;
        }

        private static RefundGatewayResult success(String refundTransactionNo, String message) {
            return new RefundGatewayResult(true, false, refundTransactionNo, message);
        }

        private static RefundGatewayResult failed(String message) {
            return new RefundGatewayResult(false, false, null, message);
        }

        private static RefundGatewayResult uncertain(String message) {
            return new RefundGatewayResult(false, true, null, message);
        }

        private boolean isSuccess() {
            return success;
        }

        private boolean isUncertain() {
            return uncertain;
        }

        private String getRefundTransactionNo() {
            return refundTransactionNo;
        }

        private String getMessage() {
            return message;
        }
    }    private static class PaidMarkResult {
        private final AdRechargeOrder order;
        private final boolean duplicate;

        private PaidMarkResult(AdRechargeOrder order, boolean duplicate) {
            this.order = order;
            this.duplicate = duplicate;
        }

        private AdRechargeOrder getOrder() {
            return order;
        }

        private boolean isDuplicate() {
            return duplicate;
        }
    }
}
