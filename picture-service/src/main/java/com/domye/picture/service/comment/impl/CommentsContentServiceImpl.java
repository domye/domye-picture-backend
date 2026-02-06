package com.domye.picture.service.comment.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.model.entity.comment.CommentsContent;
import com.domye.picture.service.comment.CommentsContentService;
import com.domye.picture.service.mapper.CommentsContentMapper;
import org.springframework.stereotype.Service;

/**
* @author Domye
* @description 针对表【comments_content】的数据库操作Service实现
* @createDate 2026-02-06 12:39:45
*/
@Service
public class CommentsContentServiceImpl extends ServiceImpl<CommentsContentMapper, CommentsContent>
    implements CommentsContentService{

    @Override
    public void addCommentContent(Long commentId, String content) {
        CommentsContent commentsContent = new CommentsContent();
        commentsContent.setCommentid(commentId);
        commentsContent.setCommenttext(content);
        save(commentsContent);
    }
}




