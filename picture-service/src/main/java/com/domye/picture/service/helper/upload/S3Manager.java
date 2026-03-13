package com.domye.picture.service.helper.upload;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Manager {
    final S3ClientConfig s3ClientConfig;
    final S3Client s3Client;

    /**
     * 上传文件
     *
     * @param key  对象键（文件在存储桶中的唯一标识）
     * @param file 要上传的本地文件对象
     */
    public void putObject(String key, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3ClientConfig.getBucket())
                .key(normalizeKey(key))
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    /**
     * 删除对象（支持传入 URL 或 key）
     *
     * @param urlOrKey 对象键或完整 URL
     */
    public void deleteObject(String urlOrKey) {
        String key = extractKeyFromUrl(urlOrKey);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3ClientConfig.getBucket())
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * 获取文件访问URL
     *
     * @param key 对象键
     * @return 访问URL
     */
    public String getObjectUrl(String key) {
        return s3ClientConfig.getHost() + "/" + normalizeKey(key);
    }

    /**
     * 从完整 URL 中提取 key
     *
     * @param urlOrKey 完整 URL 或 key
     * @return key
     */
    private String extractKeyFromUrl(String urlOrKey) {
        if (StrUtil.isBlank(urlOrKey)) {
            return urlOrKey;
        }
        String host = s3ClientConfig.getHost();
        if (urlOrKey.startsWith(host)) {
            // 从 URL 中提取 path 部分
            String key = urlOrKey.substring(host.length());
            // 移除开头的斜杠
            return normalizeKey(key);
        }
        return normalizeKey(urlOrKey);
    }

    /**
     * 标准化 key，移除开头的斜杠
     */
    private String normalizeKey(String key) {
        if (StrUtil.isNotBlank(key) && key.startsWith("/")) {
            return key.substring(1);
        }
        return key;
    }
}