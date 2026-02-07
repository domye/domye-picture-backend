package com.domye.picture.service.api.comment;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.dto.comment.CommentQueryRequest;
import com.domye.picture.model.entity.comment.Comments;
import com.domye.picture.model.vo.comment.CommentReplyVO;
import com.domye.picture.model.vo.comment.CommentVO;
import com.domye.picture.service.helper.comment.DataMaps;
import com.domye.picture.service.helper.comment.IdCollection;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author Domye
 * @description 针对表【comments】的数据库操作Service
 * @createDate 2026-02-06 12:43:06
 */
public interface CommentsService extends IService<Comments> {

    Long addComment(CommentAddRequest commentAddRequest, Long userId, HttpServletRequest request);

    Page<CommentVO> listTopCommentsWithPreview(CommentQueryRequest request);

    <T> List<Long> extractIds(List<T> items, java.util.function.Function<T, Long> mapper);

    Map<Long, List<Comments>> fetchAndGroupReplies(List<Long> rootIds, int limit);


    IdCollection collectAllIds(List<Comments> firstLevelComments, Map<Long, List<Comments>> repliesMap);

    DataMaps buildDataMaps(IdCollection idCollection);

    List<CommentVO> buildCommentVOList(List<Comments> firstLevelComments,
                                       Map<Long, List<Comments>> repliesMap,
                                       DataMaps dataMaps);

    CommentVO buildCommentVO(Comments comment, DataMaps dataMaps);

    List<CommentReplyVO> buildReplyVOList(List<Comments> replies, DataMaps dataMaps);
}

