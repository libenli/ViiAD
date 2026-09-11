package com.mrd.ad.business.report.service;

import com.mrd.ad.business.report.domain.AdPlayLog;
import com.mrd.ad.business.report.dto.AdPlayLogQuery;
import com.mrd.ad.business.report.dto.AdPlayReportRequest;
import com.mrd.ad.business.report.dto.AdPlayReportResult;
import com.mrd.ad.business.report.dto.ReportOverview;
import com.mrd.ad.common.core.PageResult;

public interface AdReportService {

    PageResult<AdPlayLog> pagePlayLogs(AdPlayLogQuery query);

    ReportOverview overview(AdPlayLogQuery query);

    AdPlayReportResult reportDevicePlay(AdPlayReportRequest request);

    void createDemoPlayLog(Long planId, Long deviceId);
}
