package com.mrd.ad.business.material.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.material.domain.AdMaterial;
import com.mrd.ad.business.material.dto.AdMaterialCreateRequest;
import com.mrd.ad.business.material.dto.AdMaterialQuery;
import com.mrd.ad.business.material.dto.AdMaterialUpdateRequest;
import com.mrd.ad.business.material.mapper.AdMaterialMapper;
import com.mrd.ad.business.material.service.AdMaterialService;
import com.mrd.ad.business.system.service.SysDataScopeService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class AdMaterialServiceImpl implements AdMaterialService {

    private final AdMaterialMapper adMaterialMapper;
    private final SysDataScopeService dataScopeService;

    public AdMaterialServiceImpl(AdMaterialMapper adMaterialMapper, SysDataScopeService dataScopeService) {
        this.adMaterialMapper = adMaterialMapper;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public PageResult<AdMaterial> page(AdMaterialQuery query) {
        LambdaQueryWrapper<AdMaterial> wrapper = new LambdaQueryWrapper<AdMaterial>()
                .eq(AdMaterial::getDeleted, 0)
                .orderByDesc(AdMaterial::getCreateTime);
        applyDataScope(wrapper);

        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(item -> item
                    .like(AdMaterial::getMaterialName, query.getKeyword())
                    .or()
                    .like(AdMaterial::getMaterialCode, query.getKeyword()));
        }
        if (query.getAdId() != null) {
            wrapper.eq(AdMaterial::getAdId, query.getAdId());
        }
        if (StringUtils.isNotBlank(query.getMaterialType())) {
            wrapper.eq(AdMaterial::getMaterialType, query.getMaterialType());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdMaterial::getStatus, query.getStatus());
        }
        if (query.getUploaderId() != null) {
            wrapper.eq(AdMaterial::getUploaderId, query.getUploaderId());
        }

        Page<AdMaterial> page = adMaterialMapper.selectPage(new Page<AdMaterial>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdMaterial>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdMaterial getDetail(Long id) {
        AdMaterial material = adMaterialMapper.selectById(id);
        if (material == null || Integer.valueOf(1).equals(material.getDeleted())) {
            throw new BusinessException(404, "素材不存在");
        }
        dataScopeService.assertAdIdVisible(material.getAdId());
        return material;
    }

    @Override
    public AdMaterial create(AdMaterialCreateRequest request) {
        Date now = new Date();
        AdMaterial material = new AdMaterial();
        copyRequest(request, material);
        dataScopeService.assertAdIdVisible(material.getAdId());
        material.setMaterialCode(nextMaterialCode(now));
        material.setStatus("draft");
        material.setUploaderId(dataScopeService.currentUserId());
        material.setCreateBy(dataScopeService.currentUserId());
        material.setCreateTime(now);
        material.setUpdateTime(now);
        material.setDeleted(0);
        adMaterialMapper.insert(material);
        return material;
    }

    @Override
    public AdMaterial update(Long id, AdMaterialUpdateRequest request) {
        AdMaterial material = getDetail(id);
        if (!"draft".equals(material.getStatus()) && !"rejected".equals(material.getStatus())) {
            throw new BusinessException("只有草稿或已驳回素材可以编辑");
        }
        dataScopeService.assertAdIdVisible(request.getAdId());
        copyRequest(request, material);
        material.setUpdateBy(dataScopeService.currentUserId());
        material.setUpdateTime(new Date());
        adMaterialMapper.updateById(material);
        return material;
    }

    @Override
    public AdMaterial submit(Long id) {
        AdMaterial material = getDetail(id);
        if (!"draft".equals(material.getStatus()) && !"rejected".equals(material.getStatus())) {
            throw new BusinessException("当前状态不能提交审核");
        }
        material.setStatus("pending_review");
        material.setUpdateBy(dataScopeService.currentUserId());
        material.setUpdateTime(new Date());
        adMaterialMapper.updateById(material);
        return material;
    }

    @Override
    public AdMaterial approve(Long id, String comment) {
        AdMaterial material = getDetail(id);
        material.setStatus("approved");
        material.setReviewUserId(dataScopeService.currentUserId());
        material.setReviewComment(StringUtils.defaultIfBlank(comment, "审核通过"));
        material.setReviewTime(new Date());
        material.setUpdateBy(dataScopeService.currentUserId());
        material.setUpdateTime(new Date());
        adMaterialMapper.updateById(material);
        return material;
    }

    @Override
    public AdMaterial reject(Long id, String comment) {
        if (StringUtils.isBlank(comment)) {
            throw new BusinessException("驳回原因不能为空");
        }
        AdMaterial material = getDetail(id);
        material.setStatus("rejected");
        material.setReviewUserId(dataScopeService.currentUserId());
        material.setReviewComment(comment);
        material.setReviewTime(new Date());
        material.setUpdateBy(dataScopeService.currentUserId());
        material.setUpdateTime(new Date());
        adMaterialMapper.updateById(material);
        return material;
    }

    private void copyRequest(AdMaterialCreateRequest request, AdMaterial material) {
        material.setAdId(request.getAdId());
        material.setMaterialName(request.getMaterialName());
        material.setMaterialType(request.getMaterialType());
        material.setFileUrl(request.getFileUrl());
        material.setFileSize(request.getFileSize());
        material.setDurationSeconds(request.getDurationSeconds());
        material.setWidth(request.getWidth());
        material.setHeight(request.getHeight());
        material.setCoverUrl(request.getCoverUrl());
    }

    private String nextMaterialCode(Date date) {
        return "MAT" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date);
    }

    private void applyDataScope(LambdaQueryWrapper<AdMaterial> wrapper) {
        List<Long> adIds = dataScopeService.scopedAdIds();
        if (adIds == null) {
            return;
        }
        if (adIds.isEmpty()) {
            wrapper.eq(AdMaterial::getId, -1L);
            return;
        }
        wrapper.in(AdMaterial::getAdId, adIds);
    }
}
