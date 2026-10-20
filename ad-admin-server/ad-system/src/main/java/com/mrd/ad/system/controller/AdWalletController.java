package com.mrd.ad.system.controller;

import com.mrd.ad.business.wallet.domain.AdBillingRule;
import com.mrd.ad.business.wallet.domain.AdPayCallbackLog;
import com.mrd.ad.business.wallet.domain.AdPlayCharge;
import com.mrd.ad.business.wallet.domain.AdRechargeOrder;
import com.mrd.ad.business.wallet.domain.AdRefundOrder;
import com.mrd.ad.business.wallet.domain.AdRefundReconcileAction;
import com.mrd.ad.business.wallet.domain.AdWalletAccount;
import com.mrd.ad.business.wallet.domain.AdWalletTransaction;
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
import com.mrd.ad.business.wallet.service.AdWalletService;
import com.mrd.ad.common.annotation.RequiresPermission;
import com.mrd.ad.common.core.ApiResult;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.logging.annotation.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class AdWalletController {

    private final AdWalletService adWalletService;

    public AdWalletController(AdWalletService adWalletService) {
        this.adWalletService = adWalletService;
    }

    @OperLog(module = "预付费账户", businessType = "QUERY")
    @RequiresPermission("wallet:view")
    @GetMapping("/accounts")
    public ApiResult<PageResult<AdWalletAccount>> accounts(WalletAccountQuery query) {
        return ApiResult.success(adWalletService.pageAccounts(query));
    }

    @OperLog(module = "预付费账户", businessType = "CREATE")
    @RequiresPermission("wallet:recharge:create,wallet:recharge:audit")
    @PostMapping("/accounts/ensure")
    public ApiResult<AdWalletAccount> ensureAccount(@RequestBody WalletAccountQuery request) {
        return ApiResult.success(adWalletService.ensureAccount(request.getOwnerType(), request.getOwnerId()));
    }

    @OperLog(module = "充值订单", businessType = "QUERY")
    @RequiresPermission("wallet:recharge:view")
    @GetMapping("/recharges")
    public ApiResult<PageResult<AdRechargeOrder>> rechargeOrders(RechargeOrderQuery query) {
        return ApiResult.success(adWalletService.pageRechargeOrders(query));
    }

    @OperLog(module = "支付回调日志", businessType = "QUERY")
    @RequiresPermission("wallet:recharge:audit")
    @GetMapping("/pay-callback-logs")
    public ApiResult<PageResult<AdPayCallbackLog>> payCallbackLogs(PayCallbackLogQuery query) {
        return ApiResult.success(adWalletService.pagePayCallbackLogs(query));
    }
    @OperLog(module = "退款申请", businessType = "QUERY")
    @RequiresPermission("wallet:refund:view,wallet:refund:create,wallet:refund:audit")
    @GetMapping("/refunds")
    public ApiResult<PageResult<AdRefundOrder>> refundOrders(RefundOrderQuery query) {
        return ApiResult.success(adWalletService.pageRefundOrders(query));
    }

    @OperLog(module = "退款申请", businessType = "CREATE")
    @RequiresPermission("wallet:refund:create")
    @PostMapping("/refunds")
    public ApiResult<AdRefundOrder> createRefund(@Validated @RequestBody CreateRefundOrderRequest request) {
        return ApiResult.success(adWalletService.createRefundOrder(request));
    }

    @OperLog(module = "退款审核", businessType = "CONFIRM")
    @RequiresPermission("wallet:refund:audit")
    @PostMapping("/refunds/{id}/approve")
    public ApiResult<AdRefundOrder> approveRefund(@PathVariable Long id, @RequestBody AuditRefundOrderRequest request) {
        return ApiResult.success(adWalletService.approveRefundOrder(id, request));
    }

    @OperLog(module = "退款审核", businessType = "REJECT")
    @RequiresPermission("wallet:refund:audit")
    @PostMapping("/refunds/{id}/reject")
    public ApiResult<AdRefundOrder> rejectRefund(@PathVariable Long id, @RequestBody AuditRefundOrderRequest request) {
        return ApiResult.success(adWalletService.rejectRefundOrder(id, request));
    }

        @OperLog(module = "退款对账", businessType = "CONFIRM")
    @RequiresPermission("wallet:refund:audit")
    @PostMapping("/refunds/{id}/sync")
    public ApiResult<AdRefundOrder> syncRefund(@PathVariable Long id) {
        return ApiResult.success(adWalletService.syncRefundOrder(id));
    }

    @OperLog(module = "退款异常处理", businessType = "QUERY")
    @RequiresPermission("wallet:refund:audit")
    @GetMapping("/refunds/{id}/reconcile-actions")
    public ApiResult<List<AdRefundReconcileAction>> refundReconcileActions(@PathVariable Long id) {
        return ApiResult.success(adWalletService.listRefundReconcileActions(id));
    }

    @OperLog(module = "退款异常处理", businessType = "CONFIRM")
    @RequiresPermission("wallet:refund:audit")
    @PostMapping("/refunds/{id}/reconcile-actions")
    public ApiResult<AdRefundOrder> handleRefundReconcileAction(
            @PathVariable Long id,
            @Validated @RequestBody RefundReconcileActionRequest request) {
        return ApiResult.success(adWalletService.handleRefundReconcileAction(id, request));
    }
@OperLog(module = "充值订单", businessType = "CREATE")
    @RequiresPermission("wallet:recharge:create")
    @PostMapping("/recharges")
    public ApiResult<AdRechargeOrder> createRecharge(@Validated @RequestBody CreateRechargeOrderRequest request) {
        return ApiResult.success(adWalletService.createRechargeOrder(request));
    }

    @OperLog(module = "充值订单", businessType = "CREATE")
    @RequiresPermission("wallet:recharge:create")
    @PostMapping("/recharges/scan-pay")
    public ApiResult<RechargePaymentResult> createScanPayRecharge(@Validated @RequestBody CreateRechargeOrderRequest request,
                                                                  HttpServletRequest servletRequest) {
        return ApiResult.success(adWalletService.createRechargePayment(request, clientIp(servletRequest)));
    }

    @OperLog(module = "充值订单", businessType = "CONFIRM")
    @RequiresPermission("wallet:recharge:audit")
    @PostMapping("/recharges/{id}/confirm")
    public ApiResult<AdRechargeOrder> confirmRecharge(@PathVariable Long id, @RequestBody ConfirmRechargeRequest request) {
        return ApiResult.success(adWalletService.confirmRechargeOrder(id, request));
    }

    @OperLog(module = "资金流水", businessType = "QUERY")
    @RequiresPermission("wallet:view")
    @GetMapping("/transactions")
    public ApiResult<PageResult<AdWalletTransaction>> transactions(WalletTransactionQuery query) {
        return ApiResult.success(adWalletService.pageTransactions(query));
    }

    @OperLog(module = "计费规则", businessType = "QUERY")
    @RequiresPermission("wallet:rule:manage")
    @GetMapping("/billing-rules")
    public ApiResult<PageResult<AdBillingRule>> billingRules(BillingRuleQuery query) {
        return ApiResult.success(adWalletService.pageBillingRules(query));
    }

    @OperLog(module = "计费规则", businessType = "CREATE")
    @RequiresPermission("wallet:rule:manage")
    @PostMapping("/billing-rules")
    public ApiResult<AdBillingRule> createBillingRule(@Validated @RequestBody BillingRuleSaveRequest request) {
        return ApiResult.success(adWalletService.createBillingRule(request));
    }

    @OperLog(module = "计费规则", businessType = "UPDATE")
    @RequiresPermission("wallet:rule:manage")
    @PutMapping("/billing-rules/{id}")
    public ApiResult<AdBillingRule> updateBillingRule(@PathVariable Long id, @Validated @RequestBody BillingRuleSaveRequest request) {
        return ApiResult.success(adWalletService.updateBillingRule(id, request));
    }

    @OperLog(module = "播放扣费", businessType = "QUERY")
    @RequiresPermission("wallet:charge:view")
    @GetMapping("/play-charges")
    public ApiResult<PageResult<AdPlayCharge>> playCharges(PlayChargeQuery query) {
        return ApiResult.success(adWalletService.pagePlayCharges(query));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && forwarded.trim().length() > 0) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && realIp.trim().length() > 0) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
