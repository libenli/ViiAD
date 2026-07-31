package com.mrd.ad.system.controller;

import com.mrd.ad.business.file.dto.FileUploadResult;
import com.mrd.ad.business.file.service.FileUploadService;
import com.mrd.ad.common.annotation.RequiresPermission;
import com.mrd.ad.common.core.ApiResult;
import com.mrd.ad.logging.annotation.OperLog;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @OperLog(module = "文件上传", businessType = "UPLOAD")
    @RequiresPermission("material:edit")
    @PostMapping("/upload")
    public ApiResult<FileUploadResult> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "folder", required = false) String folder) {
        return ApiResult.success(fileUploadService.upload(file, folder));
    }
}
