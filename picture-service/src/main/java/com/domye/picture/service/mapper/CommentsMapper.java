package com.domye.picture.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domye.picture.model.entity.comment.Comments;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author Domye
* @description 针对表【comments】的数据库操作Mapper
* @createDate 2026-02-06 12:43:06
* @Entity com.domye.picture.model.entity.comment.Comments
*/
public interface CommentsMapper extends BaseMapper<Comments> {
    // 在 CommentsMapper.java 中添加
    @Update("UPDATE comments SET replycount = replycount + 1 WHERE commentid = #{parentId}")
    int incrementReplyCount(@Param("parentId") Long parentId);

}




