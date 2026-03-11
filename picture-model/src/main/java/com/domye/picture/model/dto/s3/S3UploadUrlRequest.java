package com.domye.picture.model.dto.s3;

import lombok.Data;

import java.io.Serializable;

@Data
public class S3UploadUrlRequest implements Serializable {
    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型（MIME类型，如 image/jpeg）
     */
    private String contentType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 上传路径前缀（可选）
     */
    private String pathPrefix;
}
