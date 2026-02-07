package com.domye.picture.service.helper.comment;

import lombok.Data;

import java.util.Set;

@Data
public class IdCollection {
    Set<Long> userIds;
    Set<Long> commentIds;

    public IdCollection(Set<Long> userIds, Set<Long> commentIds) {
        this.userIds = userIds;
        this.commentIds = commentIds;
    }
}
