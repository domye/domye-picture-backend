package com.domye.picture.common.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 图片模块常量
 */
public interface PictureConstant {

    /**
     * 分页默认最大页大小
     */
    long MAX_PAGE_SIZE = 20L;

    /**
     * 颜色搜索最多返回条数
     */
    int MAX_COLOR_SEARCH_RESULTS = 20;

    /**
     * 默认图片标签
     */
    List<String> DEFAULT_TAG_LIST = Arrays.asList("热门", "生活", "扫街", "艺术", "旅游", "创意");

    /**
     * 默认图片分类
     */
    List<String> DEFAULT_CATEGORY_LIST = Arrays.asList("人像", "风光", "扫街");
}
