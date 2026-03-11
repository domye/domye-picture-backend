package com.domye.picture.api.controller;


import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.s3.S3UploadRequest;
import com.domye.picture.model.vo.s3.S3UploadResultVO;
import com.domye.picture.service.api.s3.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class S3Controller implements Serializable {
    final S3Service s3Service;
    private HttpServletRequest httpRequest;

    /**
     * 上传文件到S3兼容存储（RustFS）
     *
     * @param file    上传的文件
     * @param request 上传请求参数
     * @return 上传结果
     */
    @Operation(summary = "上传文件到S3兼容存储")
    @PostMapping("/upload")
    public BaseResponse<S3UploadResultVO> uploadFile(
            @RequestPart("file") MultipartFile file,
            S3UploadRequest request,
            HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
        Throw.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        S3UploadResultVO result = s3Service.uploadFile(file, request);
        return Result.success(result);
    }

    /**
     * 删除S3上的文件
     *
     * @param objectKey 对象键
     * @return 是否删除成功
     */
    @Operation(summary = "删除S3上的文件")
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteFile(@RequestParam("objectKey") String objectKey) {
        Throw.throwIf(objectKey == null || objectKey.isEmpty(), ErrorCode.PARAMS_ERROR, "对象键不能为空");
        s3Service.deleteFile(objectKey);
        return Result.success(true);
    }
}
