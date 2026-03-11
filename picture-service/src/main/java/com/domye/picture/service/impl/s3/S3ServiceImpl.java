package com.domye.picture.service.impl.s3;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.domye.picture.common.exception.BusinessException;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.s3.S3UploadRequest;
import com.domye.picture.model.vo.s3.S3UploadResultVO;
import com.domye.picture.service.api.s3.S3Service;
import com.domye.picture.service.helper.upload.S3ClientConfig;
import com.domye.picture.service.helper.upload.S3Manager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {
    final S3ClientConfig s3ClientConfig;
    final S3Manager s3Manager;

    private static final Long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "txt", "zip", "rar", "mp4", "mp3", "wav"
    );

    @Override
    public S3UploadResultVO uploadFile(MultipartFile file, S3UploadRequest request) {
        // 校验文件
        validateFile(file);

        // 生成文件路径
        String objectKey = generateObjectKey(file, request);

        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = File.createTempFile("s3_upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile);

            // 上传到S3
            PutObjectResponse response = s3Manager.putObject(objectKey, tempFile);

            // 构建返回结果
            S3UploadResultVO result = new S3UploadResultVO();
            result.setUrl(s3Manager.getObjectUrl(objectKey));
            result.setFileName(file.getOriginalFilename());
            result.setFileSize(file.getSize());
            result.setContentType(file.getContentType());
            result.setObjectKey(objectKey);

            return result;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            deleteTempFile(tempFile);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        Throw.throwIf(objectKey == null || objectKey.isEmpty(), ErrorCode.PARAMS_ERROR, "对象键不能为空");
        s3Manager.deleteObject(objectKey);
    }

    private void validateFile(MultipartFile file) {
        Throw.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        Throw.throwIf(file.getSize() > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件大小不能超过10MB");

        String extension = FileUtil.getSuffix(file.getOriginalFilename());
        Throw.throwIf(extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase()),
                ErrorCode.PARAMS_ERROR, "不支持的文件类型");
    }

    private String generateObjectKey(MultipartFile file, S3UploadRequest request) {
        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.getSuffix(originalFilename);

        // 生成文件名：日期_随机字符串.后缀
        String dateStr = DateUtil.formatDate(new Date()).replace("-", "");
        String randomStr = RandomUtil.randomString(16);

        String fileName;
        if (request != null && request.getFileName() != null && !request.getFileName().isEmpty()) {
            fileName = request.getFileName();
            if (!fileName.endsWith("." + extension)) {
                fileName = fileName + "." + extension;
            }
        } else {
            fileName = String.format("%s_%s.%s", dateStr, randomStr, extension);
        }

        // 拼接路径
        String pathPrefix = "uploads";
        if (request != null && request.getPathPrefix() != null && !request.getPathPrefix().isEmpty()) {
            pathPrefix = request.getPathPrefix().replaceAll("^/+|/+$", "");
        }

        return String.format("%s/%s", pathPrefix, fileName);
    }

    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                log.warn("临时文件删除失败: {}", file.getAbsolutePath());
            }
        }
    }
}
