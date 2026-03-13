package com.domye.picture.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceUsageAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 已使用大小
     */
    private Long usedSize;
    /**
     * 总大小
     */
    private Long maxSize;
    /**
     * 空间使用比例
     */
    private Double sizeUsageRatio;
    /**
     * 当前图片数量
     */
    private Long usedCount;
    /**
     * 最大图片数量
     */
    private Long maxCount;
    /**
     * 图片数量占比
     */
    private Double countUsageRatio;
}