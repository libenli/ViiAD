package com.mrd.ad.business.system.service;

import com.mrd.ad.business.system.dto.SysDictOption;

import java.util.List;
import java.util.Map;

public interface SysDictService {

    Map<String, List<SysDictOption>> listAll();

    List<SysDictOption> listByCode(String code);
}
