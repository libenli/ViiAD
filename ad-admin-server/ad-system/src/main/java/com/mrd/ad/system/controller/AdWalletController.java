package com.mrd.ad.system.controller;

import com.mrd.ad.business.wallet.domain.AdBillingRule;
import com.mrd.ad.business.wallet.domain.AdPlayCharge;
import com.mrd.ad.business.wallet.domain.AdRechargeOrder;
import com.mrd.ad.business.wallet.domain.AdWalletAccount;
import com.mrd.ad.business.wallet.domain.AdWalletTransaction;
import com.mrd.ad.business.wallet.dto.BillingRuleQuery;
import com.mrd.ad.business.wallet.dto.BillingRuleSaveRequest;
import com.mrd.ad.business.wallet.dto.ConfirmRechargeRequest;
import com.mrd.ad.business.wallet.dto.CreateRechargeOrderRequest;
import com.mrd.ad.business.wallet.dto.PlayChargeQuery;
import com.mrd.ad.business.wallet.dto.RechargeOrderQuery;
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

@RestController
@RequestMapping("/api/wallet")
public class AdWalletController {

    private final AdWalletService adWalletService;

    public AdWalletController(AdWalletService adWalletService) {
        this.adWalletService = adWalletService;
    }

    @OperLog(module = "预付费账户", businessType = "QUERY")
    @GetMapping("/accounts")
    public ApiResult<PageResult<AdWalletAccount>> accounts(WalletAccountQuery query) {
        return ApiResult.success(adWalletService.pageAccounts(query));
    }

    @OperLog(module = "预付费账户", businessType = "CREATE")
    @RequiresPermission("bill:confirm")
    @PostMapping("/accounts/ensure")
    public ApiResult<AdWalletAccount> ensureAccount(@RequestBody WalletAccountQuery request) {
        return ApiResult.success(adWalletService.ensureAccount(request.getOwnerType(), request.getOwnerId()));
    }

    @OperLog(module = "充值订单", businessType = "QUERY")
    @GetMapping("/recharges")
    public ApiResult<PageResult<AdRechargeOrder>> rechargeOrders(RechargeOrderQuery query) {
        return ApiResult.success(adWalletService.pageRechargeOrders(query));
    }

    @OperLog(module = "充值订单", businessType = "CREATE")
    @PostMapping("/recharges")
    public ApiResult<AdRechargeOrder> createRecharge(@Validated @RequestBody CreateRechargeOrderRequest request) {
        return ApiResult.success(adWalletService.createRechargeOrder(request));
    }

    @OperLog(module = "充值订单", businessType = "CONFIRM")
    @RequiresPermission("bill:confirm")
    @PostMapping("/recharges/{id}/confirm")
    public ApiResult<AdRechargeOrder> confirmRecharge(@PathVariable Long id, @RequestBody ConfirmRechargeRequest request) {
        return ApiResult.success(adWalletService.confirmRechargeOrder(id, request));
    }

    @OperLog(module = "资金流水", businessType = "QUERY")
    @GetMapping("/transactions")
    public ApiResult<PageResult<AdWalletTransaction>> transactions(WalletTransactionQuery query) {
        return ApiResult.success(adWalletService.pageTransactions(query));
    }

    @OperLog(module = "计费规则", businessType = "QUERY")
    @GetMapping("/billing-rules")
    public ApiResult<PageResult<AdBillingRule>> billingRules(BillingRuleQuery query) {
        return ApiResult.success(adWalletService.pageBillingRules(query));
    }

    @OperLog(module = "计费规则", businessType = "CREATE")
    @RequiresPermission("bill:confirm")
    @PostMapping("/billing-rules")
    public ApiResult<AdBillingRule> createBillingRule(@Validated @RequestBody BillingRuleSaveRequest request) {
        return ApiResult.success(adWalletService.createBillingRule(request));
    }

    @OperLog(module = "计费规则", businessType = "UPDATE")
    @RequiresPermission("bill:confirm")
    @PutMapping("/billing-rules/{id}")
    public ApiResult<AdBillingRule> updateBillingRule(@PathVariable Long id, @Validated @RequestBody BillingRuleSaveRequest request) {
        return ApiResult.success(adWalletService.updateBillingRule(id, request));
    }

    @OperLog(module = "播放扣费", businessType = "QUERY")
    @GetMapping("/play-charges")
    public ApiResult<PageResult<AdPlayCharge>> playCharges(PlayChargeQuery query) {
        return ApiResult.success(adWalletService.pagePlayCharges(query));
    }
}
