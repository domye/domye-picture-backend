package com.domye.picture.model.vo.feed;

import com.domye.picture.model.vo.picture.PictureVO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 信息流响应
 */
@Data
public class FeedVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片列表
     */
    private List<PictureVO> records;

    /**
     * 下一页游标
     */
    private String nextCursor;

    /**
     * 是否有更多数据
     */
    private Boolean hasMore;
}