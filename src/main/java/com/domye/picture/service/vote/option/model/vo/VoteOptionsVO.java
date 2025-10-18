package com.domye.picture.service.vote.option.model.vo;

import com.domye.picture.service.vote.option.model.entity.VoteOptions;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class VoteOptionsVO {
    private Long id;

    /**
     * 选项内容
     */
    private String optionText;

    /**
     * 得票数
     */
    private Long voteCount;

    public static VoteOptionsVO objToVo(VoteOptions voteOptions) {
        if (voteOptions == null) {
            return null;
        }
        VoteOptionsVO voteOptionsVO = new VoteOptionsVO();
        BeanUtils.copyProperties(voteOptions, voteOptionsVO);
        return voteOptionsVO;

    }
}
