package com.domye.picture.model.dto.comment;

import com.domye.picture.common.result.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CommentReplyQueryRequest extends PageRequest {
    private Long pictureId;
    private Long commentId;
}