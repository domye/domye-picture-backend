
package com.domye.picture.api.service.vote.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 投票活动结束请求
 */
@Data
public class VoteEndRequest implements Serializable {
    /**
     * 投票活动ID
     */
    private Long activityId;
}
