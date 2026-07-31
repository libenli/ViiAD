package com.mrd.ad.business.bill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.ad.domain.AdOrder;
import com.mrd.ad.business.ad.mapper.AdOrderMapper;
import com.mrd.ad.business.bill.domain.AdBill;
import com.mrd.ad.business.bill.domain.AdBillDetail;
import com.mrd.ad.business.bill.dto.AdBillQuery;
import com.mrd.ad.business.bill.mapper.AdBillDetailMapper;
import com.mrd.ad.business.bill.mapper.AdBillMapper;
import com.mrd.ad.business.bill.service.AdBillService;
import com.mrd.ad.business.report.domain.AdPlayLog;
import com.mrd.ad.business.report.mapper.AdPlayLogMapper;
import com.mrd.ad.business.system.service.SysDataScopeService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdBillServiceImpl implements AdBillService {

    private static final BigDecimal UNIT_PRICE = new BigDecimal("0.08");

    private final AdBillMapper adBillMapper;
    private final AdBillDetailMapper adBillDetailMapper;
    private final AdPlayLogMapper adPlayLogMapper;
    private final AdOrderMapper adOrderMapper;
    private final SysDataScopeService dataScopeService;

    public AdBillServiceImpl(AdBillMapper adBillMapper,
                             AdBillDetailMapper adBillDetailMapper,
                             AdPlayLogMapper adPlayLogMapper,
                             AdOrderMapper adOrderMapper,
                             SysDataScopeService dataScopeService) {
        this.adBillMapper = adBillMapper;
        this.adBillDetailMapper = adBillDetailMapper;
        this.adPlayLogMapper = adPlayLogMapper;
        this.adOrderMapper = adOrderMapper;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public PageResult<AdBill> page(AdBillQuery query) {
        LambdaQueryWrapper<AdBill> wrapper = new LambdaQueryWrapper<AdBill>()
                .orderByDesc(AdBill::getCreateTime);
        applyDataScope(wrapper);
        if (StringUtils.isNotBlank(query.getBillMonth())) {
            wrapper.eq(AdBill::getBillMonth, query.getBillMonth());
        }
        if (StringUtils.isNotBlank(query.getBillType())) {
            wrapper.eq(AdBill::getBillType, query.getBillType());
        }
        if (query.getAdvertiserId() != null) {
            wrapper.eq(AdBill::getAdvertiserId, query.getAdvertiserId());
        }
        if (query.getAgentId() != null) {
            wrapper.eq(AdBill::getAgentId, query.getAgentId());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdBill::getStatus, query.getStatus());
        }
        Page<AdBill> page = adBillMapper.selectPage(new Page<AdBill>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdBill>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdBill getDetail(Long id) {
        AdBill bill = adBillMapper.selectById(id);
        if (bill == null) {
            throw new BusinessException(404, "账单不存在");
        }
        dataScopeService.assertAdvertiserAgentVisible(bill.getAdvertiserId(), bill.getAgentId());
        return bill;
    }

    @Override
    public List<AdBillDetail> getDetails(Long billId) {
        getDetail(billId);
        return adBillDetailMapper.selectList(new LambdaQueryWrapper<AdBillDetail>()
                .eq(AdBillDetail::getBillId, billId)
                .orderByDesc(AdBillDetail::getItemAmount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AdBill> generate(String billMonth) {
        if (StringUtils.isBlank(billMonth) || billMonth.length() != 7) {
            throw new BusinessException("账单月份格式应为 yyyy-MM");
        }
        LambdaQueryWrapper<AdPlayLog> logWrapper = new LambdaQueryWrapper<AdPlayLog>()
                .ge(AdPlayLog::getPlayDate, Date.valueOf(billMonth + "-01"))
                .lt(AdPlayLog::getPlayDate, nextMonthStart(billMonth));
        applyLogDataScope(logWrapper);
        List<AdPlayLog> logs = adPlayLogMapper.selectList(logWrapper);
        if (logs == null || logs.isEmpty()) {
            throw new BusinessException("该月份暂无播放日志，无法生成账单");
        }

        Map<Long, Map<String, BillItem>> advertiserItems = new HashMap<Long, Map<String, BillItem>>();
        for (AdPlayLog log : logs) {
            AdOrder ad = adOrderMapper.selectById(log.getAdId());
            if (ad == null || ad.getAdvertiserId() == null) {
                continue;
            }
            dataScopeService.assertAdVisible(ad);
            Map<String, BillItem> items = advertiserItems.get(ad.getAdvertiserId());
            if (items == null) {
                items = new HashMap<String, BillItem>();
                advertiserItems.put(ad.getAdvertiserId(), items);
            }
            String key = log.getAdId() + "-" + log.getPlanId();
            BillItem item = items.get(key);
            if (item == null) {
                item = new BillItem();
                item.adId = log.getAdId();
                item.planId = log.getPlanId();
                item.agentId = ad.getAgentId();
                item.itemName = ad.getAdName() + " / 计划" + log.getPlanId();
                items.put(key, item);
            }
            item.playCount += log.getPlayCount() == null ? 0 : log.getPlayCount();
        }

        List<AdBill> bills = new ArrayList<AdBill>();
        for (Map.Entry<Long, Map<String, BillItem>> entry : advertiserItems.entrySet()) {
            AdBill existing = adBillMapper.selectOne(new LambdaQueryWrapper<AdBill>()
                    .eq(AdBill::getBillMonth, billMonth)
                    .eq(AdBill::getBillType, "advertiser")
                    .eq(AdBill::getAdvertiserId, entry.getKey())
                    .last("LIMIT 1"));
            if (existing != null) {
                bills.add(existing);
                continue;
            }

            AdBill bill = new AdBill();
            bill.setBillNo(nextBillNo(billMonth, entry.getKey()));
            bill.setBillType("advertiser");
            bill.setAdvertiserId(entry.getKey());
            bill.setAgentId(firstAgentId(entry.getValue()));
            bill.setBillMonth(billMonth);
            bill.setAmountTotal(BigDecimal.ZERO);
            bill.setAmountPaid(BigDecimal.ZERO);
            bill.setStatus("pending");
            bill.setCreateTime(new java.util.Date());
            bill.setUpdateTime(new java.util.Date());
            adBillMapper.insert(bill);

            BigDecimal amountTotal = BigDecimal.ZERO;
            for (BillItem item : entry.getValue().values()) {
                BigDecimal itemAmount = UNIT_PRICE.multiply(BigDecimal.valueOf(item.playCount)).setScale(2, BigDecimal.ROUND_HALF_UP);
                amountTotal = amountTotal.add(itemAmount);

                AdBillDetail detail = new AdBillDetail();
                detail.setBillId(bill.getId());
                detail.setAdId(item.adId);
                detail.setPlanId(item.planId);
                detail.setItemName(item.itemName);
                detail.setItemCount(item.playCount);
                detail.setItemAmount(itemAmount);
                adBillDetailMapper.insert(detail);
            }
            bill.setAmountTotal(amountTotal.setScale(2, BigDecimal.ROUND_HALF_UP));
            bill.setUpdateTime(new java.util.Date());
            adBillMapper.updateById(bill);
            bills.add(bill);
        }
        return bills;
    }

    @Override
    public AdBill confirm(Long id) {
        AdBill bill = getDetail(id);
        if ("paid".equals(bill.getStatus())) {
            throw new BusinessException("已支付账单不能重复确认");
        }
        bill.setStatus("confirmed");
        bill.setConfirmTime(new java.util.Date());
        bill.setUpdateTime(new java.util.Date());
        adBillMapper.updateById(bill);
        return bill;
    }

    @Override
    public AdBill pay(Long id) {
        AdBill bill = getDetail(id);
        if ("paid".equals(bill.getStatus())) {
            throw new BusinessException("账单已支付");
        }
        if (!"confirmed".equals(bill.getStatus())) {
            bill.setConfirmTime(new java.util.Date());
        }
        bill.setStatus("paid");
        bill.setAmountPaid(bill.getAmountTotal());
        bill.setPayTime(new java.util.Date());
        bill.setUpdateTime(new java.util.Date());
        adBillMapper.updateById(bill);
        return bill;
    }

    private Date nextMonthStart(String billMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(Date.valueOf(billMonth + "-01"));
        calendar.add(Calendar.MONTH, 1);
        return new Date(calendar.getTimeInMillis());
    }

    private String nextBillNo(String billMonth, Long advertiserId) {
        return "BILL" + billMonth.replace("-", "") + advertiserId + new SimpleDateFormat("HHmmssSSS").format(new java.util.Date());
    }

    private void applyDataScope(LambdaQueryWrapper<AdBill> wrapper) {
        if (dataScopeService.isAllData()) {
            return;
        }
        if (dataScopeService.isAdvertiserScope() && dataScopeService.currentAdvertiserId() != null) {
            wrapper.eq(AdBill::getAdvertiserId, dataScopeService.currentAdvertiserId());
            return;
        }
        if (dataScopeService.isAgentScope() && dataScopeService.currentAgentId() != null) {
            wrapper.eq(AdBill::getAgentId, dataScopeService.currentAgentId());
            return;
        }
        wrapper.eq(AdBill::getId, -1L);
    }

    private void applyLogDataScope(LambdaQueryWrapper<AdPlayLog> wrapper) {
        List<Long> adIds = dataScopeService.scopedAdIds();
        if (adIds == null) {
            return;
        }
        if (adIds.isEmpty()) {
            wrapper.eq(AdPlayLog::getId, -1L);
            return;
        }
        wrapper.in(AdPlayLog::getAdId, adIds);
    }

    private Long firstAgentId(Map<String, BillItem> items) {
        for (BillItem item : items.values()) {
            if (item.agentId != null) {
                return item.agentId;
            }
        }
        return null;
    }

    private static class BillItem {
        private Long adId;
        private Long planId;
        private Long agentId;
        private String itemName;
        private int playCount;
    }
}
