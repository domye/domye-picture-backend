package com.domye.picture.model.user.dto;

import lombok.Data;

@Data
public class FilterListRequest {
    private Long userId;
    private Long type;
    private Long mode;
}
