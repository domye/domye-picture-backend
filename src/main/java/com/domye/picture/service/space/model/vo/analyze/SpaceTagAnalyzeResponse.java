package com.domye.picture.service.space.model.vo.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 标签名称
     */
    private String tag;
    /**
     * 使用次数
     */
    private Long count;
}
