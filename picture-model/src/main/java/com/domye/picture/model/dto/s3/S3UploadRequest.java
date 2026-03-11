package com.domye.picture.model.dto.s3;

import lombok.Data;

import java.io.Serializable;

@Data
public class S3UploadRequest implements Serializable {
    /**
     * 上传路径前缀（可选，不填则使用默认路径）
     */
    private String pathPrefix;

    /**
     * 自定义文件名（可选，不填则自动生成）
     */
    private String fileName;
}
