package com.domye.picture.service.helper.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.domye.picture.common.exception.BusinessException;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileManager {

    final S3ClientConfig s3ClientConfig;
    final S3Manager s3Manager;

    /**
     * 上传图片方法
     *
     * @param multipartFile    上传的图片文件
     * @param uploadPathPrefix 上传路径前缀
     * @return UploadPictureResult 上传结果对象
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址相关处理
        String uuid = RandomUtil.randomString(16);
        String originFilename = multipartFile.getOriginalFilename();
        String fileSuffix = FileUtil.getSuffix(originFilename);
        // 格式化生成新的上传文件名：日期_UUID.后缀
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, fileSuffix);

        File tempFile = null;
        File compressedFile = null;
        File thumbnailFile = null;
        try {
            // 创建临时文件
            tempFile = File.createTempFile("upload_", "." + fileSuffix);
            multipartFile.transferTo(tempFile);

            // 读取原图信息
            BufferedImage originalImage = ImageIO.read(tempFile);
            int picWidth = originalImage.getWidth();
            int picHeight = originalImage.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            String picFormat = fileSuffix.toLowerCase();

            // 生成压缩后的 jpg 文件（Thumbnailator 不支持 webp 输出）
            String compressedFilename = FileUtil.mainName(uploadFilename) + "_compressed.jpg";
            String compressedPath = String.format("/%s/%s", uploadPathPrefix, compressedFilename);
            compressedFile = File.createTempFile("compressed_", ".jpg");
            Thumbnails.of(tempFile)
                    .scale(1.0)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toFile(compressedFile);

            // 生成缩略图 (128x128)
            String thumbnailFilename = FileUtil.mainName(uploadFilename) + "_thumbnail." + fileSuffix;
            String thumbnailPath = String.format("/%s/%s", uploadPathPrefix, thumbnailFilename);
            thumbnailFile = File.createTempFile("thumbnail_", "." + fileSuffix);
            Thumbnails.of(tempFile)
                    .size(128, 128)
                    .keepAspectRatio(true)
                    .toFile(thumbnailFile);

            // 上传文件到 S3
            s3Manager.putObject(compressedPath, compressedFile);
            s3Manager.putObject(thumbnailPath, thumbnailFile);

            // 构建返回结果
            UploadPictureResult result = UploadPictureResult.builder()
                    .picName(FileUtil.mainName(originFilename))
                    .picWidth(picWidth)
                    .picHeight(picHeight)
                    .picScale(picScale)
                    .picFormat(picFormat)
                    .picSize(compressedFile.length())
                    .url(s3ClientConfig.getHost() + compressedPath)
                    .thumbnailUrl(s3ClientConfig.getHost() + thumbnailPath)
                    .picColor(extractDominantColor(originalImage))
                    .build();

            return result;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 清理临时文件
            deleteTempFile(tempFile);
            deleteTempFile(compressedFile);
            deleteTempFile(thumbnailFile);
        }
    }

    /**
     * 高性能主色提取（9点采样）
     */
    private String extractDominantColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int cx = width >> 1;
        int cy = height >> 1;

        int stepX = Math.max(1, width / 8);
        int stepY = Math.max(1, height / 8);

        int[] xs = {cx, cx - stepX, cx + stepX};
        int[] ys = {cy, cy - stepY, cy + stepY};

        int r = 0, g = 0, b = 0, count = 0;

        for (int x : xs) {
            if (x < 0 || x >= width) continue;
            for (int y : ys) {
                if (y < 0 || y >= height) continue;

                int rgb = image.getRGB(x, y);
                r += (rgb >> 16) & 0xFF;
                g += (rgb >> 8) & 0xFF;
                b += rgb & 0xFF;
                count++;
            }
        }

        if (count == 0) return "#000000";

        r /= count;
        g /= count;
        b /= count;

        return String.format("#%02X%02X%02X", r, g, b);
    }

    private void validPicture(MultipartFile file) {
        Throw.throwIf(file == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        Long fileSize = file.getSize();
        final Long ONE_MB = 1024 * 1024L;
        Throw.throwIf(fileSize > 2 * ONE_MB, ErrorCode.PARAMS_ERROR, "图片大小不能超过2M");
        String fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        Throw.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}