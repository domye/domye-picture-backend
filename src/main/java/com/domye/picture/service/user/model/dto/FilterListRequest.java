package com.domye.picture.service.user.model.dto;

import lombok.Data;

@Data
public class FilterListRequest {
    private Long userId;
    private Long type;
    private Long mode;
}
