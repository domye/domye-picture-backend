package com.domye.picture.model.space.dto.analyze;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;
}