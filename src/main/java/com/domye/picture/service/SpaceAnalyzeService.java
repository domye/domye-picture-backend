package com.domye.picture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.Space.analyze.SpaceCategoryAnalyzeRequest;
import com.domye.picture.model.dto.Space.analyze.SpaceSizeAnalyzeRequest;
import com.domye.picture.model.dto.Space.analyze.SpaceTagAnalyzeRequest;
import com.domye.picture.model.dto.Space.analyze.SpaceUsageAnalyzeRequest;
import com.domye.picture.model.entity.Space;
import com.domye.picture.model.entity.User;
import com.domye.picture.model.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.domye.picture.model.vo.space.analyze.SpaceSizeAnalyzeResponse;
import com.domye.picture.model.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.domye.picture.model.vo.space.analyze.SpaceUsageAnalyzeResponse;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {

    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User user);

    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    void checkSpaceAuth(User loginUser, Space space);

    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);
}
