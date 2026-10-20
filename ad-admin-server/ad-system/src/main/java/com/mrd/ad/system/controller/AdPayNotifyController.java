package com.mrd.ad.system.controller;

import com.mrd.ad.business.wallet.service.AdWalletService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/pay/notify")
public class AdPayNotifyController {

    private final AdWalletService adWalletService;

    public AdPayNotifyController(AdWalletService adWalletService) {
        this.adWalletService = adWalletService;
    }

    @PostMapping(value = "/wechat", consumes = {"text/xml", "application/xml", "application/xml;charset=UTF-8"}, produces = "application/xml;charset=UTF-8")
    public String wechat(@RequestBody String notifyXml) {
        return adWalletService.handleWechatNotify(notifyXml);
    }

    @PostMapping(value = "/alipay", produces = "text/plain;charset=UTF-8")
    public String alipay(@RequestParam Map<String, String> params) {
        return adWalletService.handleAlipayNotify(params);
    }
}
