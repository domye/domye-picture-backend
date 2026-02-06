package com.domye.picture.model.dto.comment;

import lombok.Data;

@Data
public class CommentAddRequest {
    private Long pictureid;

    private Long parentid;

    private String content;
}
