package com.mrd.ad.system.controller;

import com.mrd.ad.business.bill.domain.AdBill;
import com.mrd.ad.business.bill.domain.AdBillDetail;
import com.mrd.ad.business.bill.dto.AdBillGenerateRequest;
import com.mrd.ad.business.bill.dto.AdBillPayRequest;
import com.mrd.ad.business.bill.dto.AdBillQuery;
import com.mrd.ad.business.bill.service.AdBillService;
import com.mrd.ad.common.annotation.RequiresPermission;
import com.mrd.ad.common.core.ApiResult;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.logging.annotation.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class AdBillController {

    private final AdBillService adBillService;

    public AdBillController(AdBillService adBillService) {
        this.adBillService = adBillService;
    }

    @OperLog(module = "账单结算", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdBill>> page(AdBillQuery query) {
        return ApiResult.success(adBillService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResult<AdBill> detail(@PathVariable Long id) {
        return ApiResult.success(adBillService.getDetail(id));
    }

    @GetMapping("/{id}/details")
    public ApiResult<List<AdBillDetail>> details(@PathVariable Long id) {
        return ApiResult.success(adBillService.getDetails(id));
    }

    @OperLog(module = "账单结算", businessType = "GENERATE")
    @RequiresPermission("bill:confirm")
    @PostMapping("/generate")
    public ApiResult<List<AdBill>> generate(@Validated @RequestBody AdBillGenerateRequest request) {
        return ApiResult.success(adBillService.generate(request.getBillMonth()));
    }

    @OperLog(module = "账单结算", businessType = "CONFIRM")
    @RequiresPermission("bill:confirm")
    @PostMapping("/{id}/confirm")
    public ApiResult<AdBill> confirm(@PathVariable Long id) {
        return ApiResult.success(adBillService.confirm(id));
    }

    @OperLog(module = "账单结算", businessType = "PAY")
    @RequiresPermission("bill:pay")
    @PostMapping("/{id}/pay")
    public ApiResult<AdBill> pay(@PathVariable Long id, @Validated @RequestBody AdBillPayRequest request) {
        return ApiResult.success(adBillService.pay(id, request.getPaymentVoucherNo(), request.getPaymentVoucherUrl()));
    }
}
