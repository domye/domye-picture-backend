package com.domye.picture.service.api.s3;

import com.domye.picture.model.dto.s3.S3UploadRequest;
import com.domye.picture.model.dto.s3.S3UploadUrlRequest;
import com.domye.picture.model.vo.s3.S3UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    /**
     * 上传文件到S3兼容存储
     *
     * @param file   上传的文件
     * @param request 上传请求参数
     * @return 上传结果
     */
    S3UploadResultVO uploadFile(MultipartFile file, S3UploadRequest request);

    /**
     * 删除S3上的文件
     *
     * @param objectKey 对象键
     */
    void deleteFile(String objectKey);
}
