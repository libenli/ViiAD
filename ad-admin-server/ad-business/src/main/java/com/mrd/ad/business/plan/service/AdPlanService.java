package com.mrd.ad.business.plan.service;

import com.mrd.ad.business.plan.domain.AdPlan;
import com.mrd.ad.business.plan.dto.AdPlanCreateRequest;
import com.mrd.ad.business.plan.dto.AdPlanQuery;
import com.mrd.ad.business.plan.dto.AdPlanUpdateRequest;
import com.mrd.ad.common.core.PageResult;

public interface AdPlanService {

    PageResult<AdPlan> page(AdPlanQuery query);

    AdPlan getDetail(Long id);

    AdPlan create(AdPlanCreateRequest request);

    AdPlan update(Long id, AdPlanUpdateRequest request);

    AdPlan schedule(Long id);

    AdPlan start(Long id);

    AdPlan pause(Long id);

    AdPlan finish(Long id);
}
