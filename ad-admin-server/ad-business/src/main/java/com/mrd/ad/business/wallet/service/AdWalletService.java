package com.mrd.ad.business.wallet.service;

import com.mrd.ad.business.report.domain.AdPlayLog;
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
import com.mrd.ad.common.core.PageResult;

public interface AdWalletService {

    PageResult<AdWalletAccount> pageAccounts(WalletAccountQuery query);

    AdWalletAccount ensureAccount(String ownerType, Long ownerId);

    PageResult<AdRechargeOrder> pageRechargeOrders(RechargeOrderQuery query);

    AdRechargeOrder createRechargeOrder(CreateRechargeOrderRequest request);

    AdRechargeOrder confirmRechargeOrder(Long id, ConfirmRechargeRequest request);

    PageResult<AdWalletTransaction> pageTransactions(WalletTransactionQuery query);

    PageResult<AdBillingRule> pageBillingRules(BillingRuleQuery query);

    AdBillingRule createBillingRule(BillingRuleSaveRequest request);

    AdBillingRule updateBillingRule(Long id, BillingRuleSaveRequest request);

    PageResult<AdPlayCharge> pagePlayCharges(PlayChargeQuery query);

    AdPlayCharge chargePlayLog(AdPlayLog playLog);
}
