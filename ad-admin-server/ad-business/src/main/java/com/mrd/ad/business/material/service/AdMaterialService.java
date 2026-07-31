package com.mrd.ad.business.material.service;

import com.mrd.ad.business.material.domain.AdMaterial;
import com.mrd.ad.business.material.dto.AdMaterialCreateRequest;
import com.mrd.ad.business.material.dto.AdMaterialQuery;
import com.mrd.ad.business.material.dto.AdMaterialUpdateRequest;
import com.mrd.ad.common.core.PageResult;

public interface AdMaterialService {

    PageResult<AdMaterial> page(AdMaterialQuery query);

    AdMaterial getDetail(Long id);

    AdMaterial create(AdMaterialCreateRequest request);

    AdMaterial update(Long id, AdMaterialUpdateRequest request);

    AdMaterial submit(Long id);

    AdMaterial approve(Long id, String comment);

    AdMaterial reject(Long id, String comment);
}

