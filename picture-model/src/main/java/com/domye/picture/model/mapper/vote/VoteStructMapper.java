package com.domye.picture.model.mapper.vote;

import com.domye.picture.model.entity.vote.VoteActivity;
import com.domye.picture.model.entity.vote.VoteOption;
import com.domye.picture.model.vo.vote.VoteActivityVO;
import com.domye.picture.model.vo.vote.VoteActivityDetailVO;
import com.domye.picture.model.vo.vote.VoteOptionVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Vote 实体转换 Mapper
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VoteStructMapper {

    VoteStructMapper INSTANCE = Mappers.getMapper(VoteStructMapper.class);

    VoteActivityVO toVo(VoteActivity voteActivity);

    VoteActivityDetailVO toDetailVo(VoteActivity voteActivity);

    VoteOptionVO toOptionVo(VoteOption voteOption);

    List<VoteOptionVO> toOptionVoList(List<VoteOption> voteOptions);
}
