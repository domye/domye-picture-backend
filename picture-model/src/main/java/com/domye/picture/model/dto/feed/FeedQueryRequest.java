package com.domye.picture.model.dto.feed;

import com.domye.picture.model.enums.FeedTypeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 信息流查询请求
 */
@Data
public class FeedQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 信息流类型
     * 0-关注流, 1-推荐流, 2-最新流
     */
    private Integer type;

    /**
     * 游标（上一页最后一条记录的编辑时间和ID）
     * 格式: editTime_id
     */
    private String cursor;

    /**
     * 每页数量
     */
    private Integer size = 20;

    /**
     * 获取信息流类型枚举
     */
    public FeedTypeEnum getTypeEnum() {
        return FeedTypeEnum.getEnumByValue(this.type);
    }
}