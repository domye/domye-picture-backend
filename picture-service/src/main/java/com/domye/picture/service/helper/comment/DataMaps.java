package com.domye.picture.service.helper.comment;

import com.domye.picture.model.entity.comment.CommentsContent;

import com.domye.picture.model.entity.user.User;

import com.domye.picture.model.vo.comment.CommentMentionVO;

import lombok.Data;
import java.util.List;

import java.util.Map;
@Data

public class DataMaps {

    Map<Long, User> userMap;

    Map<Long, CommentsContent> contentMap;

    Map<Long, List<CommentMentionVO>> mentionsMap;



    public DataMaps(Map<Long, User> userMap, Map<Long, CommentsContent> contentMap, Map<Long, List<CommentMentionVO>> mentionsMap) {

        this.userMap = userMap;

        this.contentMap = contentMap;

        this.mentionsMap = mentionsMap;

    }

}
