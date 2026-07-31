package com.mrd.ad.business.system.service.impl;

import com.mrd.ad.business.system.dto.SysDictOption;
import com.mrd.ad.business.system.service.SysDictService;
import com.mrd.ad.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysDictServiceImpl implements SysDictService {

    private final Map<String, List<SysDictOption>> dicts;

    public SysDictServiceImpl() {
        this.dicts = buildDicts();
    }

    @Override
    public Map<String, List<SysDictOption>> listAll() {
        return dicts;
    }

    @Override
    public List<SysDictOption> listByCode(String code) {
        List<SysDictOption> items = dicts.get(code);
        if (items == null) {
            throw new BusinessException(404, "字典不存在");
        }
        return items;
    }

    private Map<String, List<SysDictOption>> buildDicts() {
        Map<String, List<SysDictOption>> map = new LinkedHashMap<String, List<SysDictOption>>();
        map.put("ad_status", list(option("草稿", "draft", "info"), option("待审核", "submitted", "warning"), option("已通过", "approved", "success"), option("已驳回", "rejected", "danger"), option("计划中", "planning", "primary"), option("投放中", "delivering", "success"), option("已暂停", "paused", "warning"), option("已结束", "finished", "info")));
        map.put("ad_type", list(option("图片广告", "image", null), option("视频广告", "video", null), option("互动广告", "interactive", null)));
        map.put("ad_objective", list(option("曝光", "exposure", null), option("转化", "conversion", null), option("互动", "engagement", null)));
        map.put("material_type", list(option("图片素材", "image", null), option("视频素材", "video", null), option("H5素材", "h5", null)));
        map.put("material_status", list(option("草稿", "draft", "info"), option("待审核", "pending_review", "warning"), option("已通过", "approved", "success"), option("已驳回", "rejected", "danger")));
        map.put("plan_schedule_status", list(option("草稿", "draft", "info"), option("待排期", "pending_schedule", "warning"), option("已排期", "scheduled", "success")));
        map.put("plan_delivery_status", list(option("未开始", "not_started", "info"), option("投放中", "live", "success"), option("已暂停", "paused", "warning"), option("已结束", "finished", "primary")));
        map.put("delivery_status", list(option("待下发", "pending", "warning"), option("成功", "success", "success"), option("失败", "failed", "danger")));
        map.put("delivery_type", list(option("自动", "auto", "primary"), option("手动", "manual", "info")));
        map.put("online_status", list(option("在线", "online", "success"), option("离线", "offline", "info")));
        map.put("fault_status", list(option("正常", "normal", "success"), option("故障", "fault", "danger")));
        map.put("device_status", list(option("启用", "active", "success"), option("停用", "disabled", "warning")));
        map.put("bill_status", list(option("待确认", "pending", "warning"), option("待支付", "confirmed", "primary"), option("已支付", "paid", "success")));
        map.put("bill_type", list(option("广告主账单", "advertiser", null), option("代理商账单", "agent", null)));
        map.put("workorder_status", list(option("待处理", "open", "warning"), option("已分派", "assigned", "primary"), option("处理中", "processing", "primary"), option("已关闭", "closed", "success")));
        map.put("workorder_priority", list(option("低", "low", "info"), option("普通", "normal", "primary"), option("高", "high", "danger")));
        map.put("workorder_source", list(option("人工创建", "manual", null), option("下发异常", "delivery", null)));
        map.put("partner_status", list(option("启用", "active", "success"), option("停用", "disabled", "warning")));
        map.put("advertiser_source", list(option("平台招商", "platform", null), option("代理商引入", "agent", null), option("直客", "direct", null)));
        return Collections.unmodifiableMap(map);
    }

    private List<SysDictOption> list(SysDictOption... options) {
        List<SysDictOption> items = new ArrayList<SysDictOption>();
        Collections.addAll(items, options);
        return Collections.unmodifiableList(items);
    }

    private SysDictOption option(String label, String value, String type) {
        return new SysDictOption(label, value, type);
    }
}
