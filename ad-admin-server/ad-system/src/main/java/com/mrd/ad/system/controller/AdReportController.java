package com.mrd.ad.system.controller;

import com.mrd.ad.business.report.domain.AdPlayLog;
import com.mrd.ad.business.report.dto.AdPlayLogQuery;
import com.mrd.ad.business.report.dto.ReportOverview;
import com.mrd.ad.business.report.service.AdReportService;
import com.mrd.ad.common.core.ApiResult;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.logging.annotation.OperLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdReportController {

    private final AdReportService adReportService;

    public AdReportController(AdReportService adReportService) {
        this.adReportService = adReportService;
    }

    @OperLog(module = "播放日志", businessType = "QUERY")
    @GetMapping("/api/play-logs")
    public ApiResult<PageResult<AdPlayLog>> playLogs(AdPlayLogQuery query) {
        return ApiResult.success(adReportService.pagePlayLogs(query));
    }

    @OperLog(module = "数据报表", businessType = "QUERY")
    @GetMapping("/api/reports/overview")
    public ApiResult<ReportOverview> overview(AdPlayLogQuery query) {
        return ApiResult.success(adReportService.overview(query));
    }
}
