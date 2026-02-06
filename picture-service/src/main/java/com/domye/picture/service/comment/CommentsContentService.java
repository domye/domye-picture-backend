package com.domye.picture.service.comment;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.entity.comment.CommentsContent;

/**
* @author Domye
* @description 针对表【comments_content】的数据库操作Service
* @createDate 2026-02-06 12:39:45
*/
public interface CommentsContentService extends IService<CommentsContent> {

    void addCommentContent(Long commentId, String content);
}
