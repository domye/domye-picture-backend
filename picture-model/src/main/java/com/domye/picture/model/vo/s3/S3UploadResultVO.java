package com.domye.picture.model.vo.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3UploadResultVO implements Serializable {
    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 对象键（存储桶中的路径）
     */
    private String objectKey;
}
