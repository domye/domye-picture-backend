package com.domye.picture.service.helper.comment;

import com.domye.picture.model.entity.comment.CommentsContent;
import com.domye.picture.model.entity.user.User;
import lombok.Data;

import java.util.Map;

@Data
public class DataMaps {
    Map<Long, User> userMap;
    Map<Long, CommentsContent> contentMap;

    public DataMaps(Map<Long, User> userMap, Map<Long, CommentsContent> contentMap) {
        this.userMap = userMap;
        this.contentMap = contentMap;
    }
}
