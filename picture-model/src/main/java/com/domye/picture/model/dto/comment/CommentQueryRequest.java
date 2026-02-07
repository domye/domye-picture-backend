package com.domye.picture.model.dto.comment;

import com.domye.picture.common.result.PageRequest;
import lombok.Data;

@Data
public class CommentQueryRequest extends PageRequest {
    private Long pictureId;           // 图片ID（必填）
    private Integer previewSize = 5;  // 预览回复数量（默认5）
}