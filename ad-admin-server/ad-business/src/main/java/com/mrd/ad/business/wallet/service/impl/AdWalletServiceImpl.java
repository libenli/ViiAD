package com.mrd.ad.business.wallet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.mrd.ad.business.wallet.mapper.AdBillingRuleMapper;
import com.mrd.ad.business.wallet.mapper.AdPlayChargeMapper;
import com.mrd.ad.business.wallet.mapper.AdRechargeOrderMapper;
import com.mrd.ad.business.wallet.mapper.AdWalletAccountMapper;
import com.mrd.ad.business.wallet.mapper.AdWalletTransactionMapper;
import com.mrd.ad.business.wallet.service.AdWalletService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class AdWalletServiceImpl implements AdWalletService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, BigDecimal.ROUND_HALF_UP);

    private final AdWalletAccountMapper accountMapper;
    private final AdRechargeOrderMapper rechargeOrderMapper;
    private final AdWalletTransactionMapper transactionMapper;
    private final AdBillingRuleMapper billingRuleMapper;
    private final AdPlayChargeMapper playChargeMapper;
    private final AdOrderMapper adOrderMapper;
    private final AdMaterialMapper adMaterialMapper;
    private final AdAdvertiserMapper advertiserMapper;
    private final AdAgentMapper agentMapper;
    private final SysDataScopeService dataScopeService;

    public AdWalletServiceImpl(AdWalletAccountMapper accountMapper,
                               AdRechargeOrderMapper rechargeOrderMapper,
                               AdWalletTransactionMapper transactionMapper,
                               AdBillingRuleMapper billingRuleMapper,
                               AdPlayChargeMapper playChargeMapper,
                               AdOrderMapper adOrderMapper,
                               AdMaterialMapper adMaterialMapper,
                               AdAdvertiserMapper advertiserMapper,
                               AdAgentMapper agentMapper,
                               SysDataScopeService dataScopeService) {
        this.accountMapper = accountMapper;
        this.rechargeOrderMapper = rechargeOrderMapper;
        this.transactionMapper = transactionMapper;
        this.billingRuleMapper = billingRuleMapper;
        this.playChargeMapper = playChargeMapper;
        this.adOrderMapper = adOrderMapper;
        this.adMaterialMapper = adMaterialMapper;
        this.advertiserMapper = advertiserMapper;
        this.agentMapper = agentMapper;
        this.dataScopeService = dataScopeService;
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
        Page<AdRechargeOrder> page = rechargeOrderMapper.selectPage(new Page<AdRechargeOrder>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdRechargeOrder>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
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

        order.setStatus("confirmed");
        order.setTransactionNo(StringUtils.defaultIfBlank(request.getTransactionNo(), order.getTransactionNo()));
        order.setRemark(StringUtils.defaultIfBlank(request.getRemark(), order.getRemark()));
        order.setAuditUserId(dataScopeService.currentUserId());
        order.setAuditTime(new Date());
        order.setPaidTime(new Date());
        order.setUpdateBy(dataScopeService.currentUserId());
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

    private void applyTransactionScope(LambdaQueryWrapper<AdWalletTransaction> wrapper) {
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
}
