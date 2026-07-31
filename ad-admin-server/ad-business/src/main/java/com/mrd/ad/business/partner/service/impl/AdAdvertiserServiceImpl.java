package com.mrd.ad.business.partner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.partner.domain.AdAdvertiser;
import com.mrd.ad.business.partner.dto.AdvertiserCreateRequest;
import com.mrd.ad.business.partner.dto.AdvertiserQuery;
import com.mrd.ad.business.partner.dto.AdvertiserUpdateRequest;
import com.mrd.ad.business.partner.mapper.AdAdvertiserMapper;
import com.mrd.ad.business.partner.service.AdAdvertiserService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class AdAdvertiserServiceImpl implements AdAdvertiserService {

    private final AdAdvertiserMapper adAdvertiserMapper;

    public AdAdvertiserServiceImpl(AdAdvertiserMapper adAdvertiserMapper) {
        this.adAdvertiserMapper = adAdvertiserMapper;
    }

    @Override
    public PageResult<AdAdvertiser> page(AdvertiserQuery query) {
        LambdaQueryWrapper<AdAdvertiser> wrapper = new LambdaQueryWrapper<AdAdvertiser>()
                .eq(AdAdvertiser::getDeleted, 0)
                .orderByDesc(AdAdvertiser::getCreateTime);
        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(item -> item
                    .like(AdAdvertiser::getAdvertiserName, query.getKeyword())
                    .or()
                    .like(AdAdvertiser::getAdvertiserCode, query.getKeyword())
                    .or()
                    .like(AdAdvertiser::getCompanyName, query.getKeyword()));
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdAdvertiser::getStatus, query.getStatus());
        }
        if (StringUtils.isNotBlank(query.getSourceType())) {
            wrapper.eq(AdAdvertiser::getSourceType, query.getSourceType());
        }
        if (query.getOwnerUserId() != null) {
            wrapper.eq(AdAdvertiser::getOwnerUserId, query.getOwnerUserId());
        }
        Page<AdAdvertiser> page = adAdvertiserMapper.selectPage(new Page<AdAdvertiser>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdAdvertiser>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdAdvertiser getDetail(Long id) {
        AdAdvertiser advertiser = adAdvertiserMapper.selectById(id);
        if (advertiser == null || Integer.valueOf(1).equals(advertiser.getDeleted())) {
            throw new BusinessException(404, "广告主不存在");
        }
        return advertiser;
    }

    @Override
    public AdAdvertiser create(AdvertiserCreateRequest request) {
        Date now = new Date();
        AdAdvertiser advertiser = new AdAdvertiser();
        copyRequest(request, advertiser);
        advertiser.setAdvertiserCode("ADV" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now));
        advertiser.setStatus("active");
        advertiser.setSourceType(StringUtils.defaultIfBlank(request.getSourceType(), "platform"));
        advertiser.setCreateBy(1L);
        advertiser.setCreateTime(now);
        advertiser.setUpdateTime(now);
        advertiser.setDeleted(0);
        adAdvertiserMapper.insert(advertiser);
        return advertiser;
    }

    @Override
    public AdAdvertiser update(Long id, AdvertiserUpdateRequest request) {
        AdAdvertiser advertiser = getDetail(id);
        copyRequest(request, advertiser);
        advertiser.setSourceType(StringUtils.defaultIfBlank(request.getSourceType(), advertiser.getSourceType()));
        advertiser.setUpdateBy(1L);
        advertiser.setUpdateTime(new Date());
        adAdvertiserMapper.updateById(advertiser);
        return advertiser;
    }

    @Override
    public AdAdvertiser enable(Long id) {
        return updateStatus(id, "active");
    }

    @Override
    public AdAdvertiser disable(Long id) {
        return updateStatus(id, "disabled");
    }

    private AdAdvertiser updateStatus(Long id, String status) {
        AdAdvertiser advertiser = getDetail(id);
        advertiser.setStatus(status);
        advertiser.setUpdateBy(1L);
        advertiser.setUpdateTime(new Date());
        adAdvertiserMapper.updateById(advertiser);
        return advertiser;
    }

    private void copyRequest(AdvertiserCreateRequest request, AdAdvertiser advertiser) {
        advertiser.setAdvertiserName(request.getAdvertiserName());
        advertiser.setCompanyName(request.getCompanyName());
        advertiser.setContactName(request.getContactName());
        advertiser.setContactPhone(request.getContactPhone());
        advertiser.setContactEmail(request.getContactEmail());
        advertiser.setOwnerUserId(request.getOwnerUserId());
        advertiser.setRemark(request.getRemark());
    }
}
