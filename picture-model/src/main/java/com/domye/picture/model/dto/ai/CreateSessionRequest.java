package com.domye.picture.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 创建会话请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 空间 ID（可选，用于限定 RAG 检索范围）
     */
    private Long spaceId;
}