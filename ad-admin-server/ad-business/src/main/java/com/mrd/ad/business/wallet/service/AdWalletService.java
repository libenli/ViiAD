package com.mrd.ad.business.wallet.service;

import com.mrd.ad.business.report.domain.AdPlayLog;
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
import com.mrd.ad.common.core.PageResult;

import java.util.List;

public interface AdWalletService {

    PageResult<AdWalletAccount> pageAccounts(WalletAccountQuery query);

    AdWalletAccount ensureAccount(String ownerType, Long ownerId);

    PageResult<AdRechargeOrder> pageRechargeOrders(RechargeOrderQuery query);

    PageResult<AdPayCallbackLog> pagePayCallbackLogs(PayCallbackLogQuery query);

    PageResult<AdRefundOrder> pageRefundOrders(RefundOrderQuery query);

    AdRefundOrder createRefundOrder(CreateRefundOrderRequest request);

    AdRefundOrder approveRefundOrder(Long id, AuditRefundOrderRequest request);

    AdRefundOrder rejectRefundOrder(Long id, AuditRefundOrderRequest request);

    AdRefundOrder syncRefundOrder(Long id);

    List<AdRefundReconcileAction> listRefundReconcileActions(Long refundOrderId);

    AdRefundOrder handleRefundReconcileAction(Long id, RefundReconcileActionRequest request);

    AdRechargeOrder createRechargeOrder(CreateRechargeOrderRequest request);

    RechargePaymentResult createRechargePayment(CreateRechargeOrderRequest request, String clientIp);

    AdRechargeOrder confirmRechargeOrder(Long id, ConfirmRechargeRequest request);

    AdRechargeOrder markRechargePaid(String rechargeNo, String payMethod, String transactionNo, java.math.BigDecimal paidAmount, String payMessage);

    String handleWechatNotify(String notifyXml);

    String handleAlipayNotify(java.util.Map<String, String> params);

    PageResult<AdWalletTransaction> pageTransactions(WalletTransactionQuery query);

    PageResult<AdBillingRule> pageBillingRules(BillingRuleQuery query);

    AdBillingRule createBillingRule(BillingRuleSaveRequest request);

    AdBillingRule updateBillingRule(Long id, BillingRuleSaveRequest request);

    PageResult<AdPlayCharge> pagePlayCharges(PlayChargeQuery query);

    AdPlayCharge chargePlayLog(AdPlayLog playLog);
}
