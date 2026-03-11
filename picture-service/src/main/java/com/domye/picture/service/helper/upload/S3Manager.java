package com.domye.picture.service.helper.upload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.InputStream;

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
     * @return PutObjectResponse 上传结果
     */
    public PutObjectResponse putObject(String key, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3ClientConfig.getBucket())
                .key(key)
                .build();
        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    /**
     * 上传文件（通过输入流）
     *
     * @param key         对象键
     * @param inputStream 输入流
     * @param contentType 内容类型
     * @param contentLength 内容长度
     * @return PutObjectResponse 上传结果
     */
    public PutObjectResponse putObject(String key, InputStream inputStream, String contentType, long contentLength) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3ClientConfig.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        return s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
    }


    /**
     * 删除对象
     *
     * @param key 对象键
     */
    public void deleteObject(String key) {
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
        return s3ClientConfig.getHost() + "/" + key;
    }
}
