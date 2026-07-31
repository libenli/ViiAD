package com.mrd.ad.business.file.service;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.mrd.ad.business.file.config.FileUploadProperties;
import com.mrd.ad.business.file.dto.FileUploadResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class FileUploadService {

    private final FileUploadProperties properties;

    public FileUploadService(FileUploadProperties properties) {
        this.properties = properties;
    }

    public FileUploadResult upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String provider = StringUtils.defaultIfBlank(properties.getProvider(), "local");
        if ("oss".equalsIgnoreCase(provider)) {
            return uploadToOss(file, folder);
        }
        return uploadToLocal(file, folder);
    }

    private FileUploadResult uploadToOss(MultipartFile file, String folder) {
        FileUploadProperties.Oss oss = properties.getOss();
        if (oss == null
                || StringUtils.isBlank(oss.getEndpoint())
                || StringUtils.isBlank(oss.getAccessKeyId())
                || StringUtils.isBlank(oss.getAccessKeySecret())
                || StringUtils.isBlank(oss.getBucketName())
                || StringUtils.isBlank(oss.getPublicBaseUrl())) {
            throw new BusinessException("OSS上传配置不完整");
        }
        String objectKey = generateObjectKey(file.getOriginalFilename(), folder);
        OSSClient ossClient = new OSSClient(normalizeEndpoint(oss.getEndpoint()), oss.getAccessKeyId(), oss.getAccessKeySecret());
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            if (StringUtils.isNotBlank(file.getContentType())) {
                metadata.setContentType(file.getContentType());
            }
            PutObjectRequest request = new PutObjectRequest(oss.getBucketName(), objectKey, file.getInputStream(), metadata);
            ossClient.putObject(request);
            return buildResult(file, objectKey, joinUrl(oss.getPublicBaseUrl(), objectKey));
        } catch (IOException ex) {
            throw new BusinessException("读取上传文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private FileUploadResult uploadToLocal(MultipartFile file, String folder) {
        String objectKey = generateObjectKey(file.getOriginalFilename(), folder);
        Path target = Paths.get(properties.getLocalPath(), objectKey).toAbsolutePath().normalize();
        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return buildResult(file, objectKey, joinUrl(properties.getPublicBaseUrl(), objectKey));
        } catch (IOException ex) {
            throw new BusinessException("保存上传文件失败");
        }
    }

    private FileUploadResult buildResult(MultipartFile file, String objectKey, String url) {
        FileUploadResult result = new FileUploadResult();
        result.setUrl(url);
        result.setObjectKey(objectKey);
        result.setOriginalFilename(file.getOriginalFilename());
        result.setContentType(file.getContentType());
        result.setSize(file.getSize());
        return result;
    }

    private String generateObjectKey(String originalFilename, String folder) {
        String extension = extension(originalFilename);
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String name = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.isNotBlank(extension)) {
            name = name + "." + extension;
        }
        return cleanPath(properties.getBasePath()) + "/" + cleanPath(folder) + "/" + datePath + "/" + name;
    }

    private String extension(String filename) {
        if (StringUtils.isBlank(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String cleanPath(String value) {
        String cleaned = StringUtils.defaultIfBlank(value, "files").replace("\\", "/").trim();
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return StringUtils.defaultIfBlank(cleaned, "files");
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        return "https://" + endpoint;
    }

    private String joinUrl(String baseUrl, String objectKey) {
        String base = StringUtils.defaultString(baseUrl);
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + objectKey;
    }
}
