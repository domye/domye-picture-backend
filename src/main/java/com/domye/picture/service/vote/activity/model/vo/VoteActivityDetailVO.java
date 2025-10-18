package com.domye.picture.service.vote.activity.model.vo;

import com.domye.picture.service.vote.activity.model.entity.VoteActivity;
import com.domye.picture.service.vote.option.model.vo.VoteOptionVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.Date;
import java.util.List;

@Data
public class VoteActivityDetailVO {

    private Long id;
    /**
     * 活动标题
     */
    private String title;

    /**
     * 创建人ID
     */
    private Long createUser;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 状态：0-未开始，1-进行中 2-已结束 3-已暂停
     */
    private Integer status;

    /**
     * 每用户最大投票数
     */
    private Integer maxVotesPerUser;

    private Long spaceId;

    /**
     * 总投票数
     */
    private Long totalVotes;


    private List<VoteOptionVO> options;

    public static VoteActivityDetailVO objToVo(VoteActivity voteActivity) {
        if (voteActivity == null) {
            return null;
        }
        VoteActivityDetailVO voteActivityVO = new VoteActivityDetailVO();
        BeanUtils.copyProperties(voteActivity, voteActivityVO);
        return voteActivityVO;
    }
}
