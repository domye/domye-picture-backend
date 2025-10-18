package com.domye.picture.service.vote.option.model.vo;

import com.domye.picture.service.vote.option.model.entity.VoteOption;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class VoteOptionVO {
    private Long id;

    /**
     * 选项内容
     */
    private String optionText;

    /**
     * 得票数
     */
    private Long voteCount;

    public static VoteOptionVO objToVo(VoteOption voteOption) {
        if (voteOption == null) {
            return null;
        }
        VoteOptionVO voteOptionVO = new VoteOptionVO();
        BeanUtils.copyProperties(voteOption, voteOptionVO);
        return voteOptionVO;

    }
}
