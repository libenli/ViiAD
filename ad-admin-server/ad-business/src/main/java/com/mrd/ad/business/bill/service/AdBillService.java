package com.mrd.ad.business.bill.service;

import com.mrd.ad.business.bill.domain.AdBill;
import com.mrd.ad.business.bill.domain.AdBillDetail;
import com.mrd.ad.business.bill.dto.AdBillQuery;
import com.mrd.ad.common.core.PageResult;

import java.util.List;

public interface AdBillService {

    PageResult<AdBill> page(AdBillQuery query);

    AdBill getDetail(Long id);

    List<AdBillDetail> getDetails(Long billId);

    List<AdBill> generate(String billMonth);

    AdBill confirm(Long id);

    AdBill pay(Long id, String paymentVoucherNo, String paymentVoucherUrl);
}
