package com.mrd.ad.business.partner.service;

import com.mrd.ad.business.partner.domain.AdAgent;
import com.mrd.ad.business.partner.dto.AgentCreateRequest;
import com.mrd.ad.business.partner.dto.AgentQuery;
import com.mrd.ad.business.partner.dto.AgentUpdateRequest;
import com.mrd.ad.common.core.PageResult;

public interface AdAgentService {

    PageResult<AdAgent> page(AgentQuery query);

    AdAgent getDetail(Long id);

    AdAgent create(AgentCreateRequest request);

    AdAgent update(Long id, AgentUpdateRequest request);

    AdAgent enable(Long id);

    AdAgent disable(Long id);
}
