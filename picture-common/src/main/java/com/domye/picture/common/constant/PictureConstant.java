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

    /**
     * 路径前缀
     */
    String PUBLIC_PATH_PREFIX = "public/%s";
    String SPACE_PATH_PREFIX = "space/%s";
    /**
     * 数据库字段名
     */
    String FIELD_NAME = "name";
    String FIELD_INTRODUCTION = "introduction";
    String FIELD_PIC_FORMAT = "picFormat";
    String FIELD_REVIEW_MESSAGE = "reviewMessage";
    String FIELD_CATEGORY = "category";
    String FIELD_PIC_WIDTH = "picWidth";
    String FIELD_PIC_HEIGHT = "picHeight";
    String FIELD_PIC_SIZE = "picSize";
    String FIELD_PIC_SCALE = "picScale";
    String FIELD_REVIEW_STATUS = "reviewStatus";
    String FIELD_REVIEWER_ID = "reviewerId";
    String FIELD_EDIT_TIME = "editTime";
    String FIELD_SPACE_ID = "spaceId";
}