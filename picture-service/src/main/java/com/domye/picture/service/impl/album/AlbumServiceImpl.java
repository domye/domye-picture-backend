package com.domye.picture.service.impl.album;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.BusinessException;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.album.AlbumAddRequest;
import com.domye.picture.model.dto.album.AlbumEditRequest;
import com.domye.picture.model.dto.album.AlbumPictureAddRequest;
import com.domye.picture.model.entity.album.Album;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.AlbumTypeEnum;
import com.domye.picture.model.mapper.album.AlbumStructMapper;
import com.domye.picture.model.vo.album.AlbumVO;
import com.domye.picture.service.api.album.AlbumService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.mapper.AlbumMapper;
import com.domye.picture.service.mapper.PictureMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album>
        implements AlbumService {

    final AlbumStructMapper albumStructMapper;
    final UserService userService;
    final PictureMapper pictureMapper;

    @Override
    public Long addAlbum(AlbumAddRequest albumAddRequest, User loginUser) {
        Long coverId = albumAddRequest.getCoverId();

        // 验证封面图片是否存在
        Throw.throwIf(coverId == null, ErrorCode.PARAMS_ERROR, "封面图不能为空");

        Picture coverPicture = pictureMapper.selectById(coverId);
        Throw.throwIf(coverPicture == null, ErrorCode.NOT_FOUND_ERROR, "封面图片不存在");

        // 验证图片权限
        Throw.throwIf(!coverPicture.getUserId().equals(loginUser.getId()) &&
                        !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR, "无权限使用该图片作为封面");

        // 检查该图片是否已经作为其他相册的主图
        Album existingAlbum = getById(coverId);
        Throw.throwIf(existingAlbum != null, ErrorCode.PARAMS_ERROR, "该图片已作为其他相册的封面");

        // 检查该图片是否已经属于其他相册
        Throw.throwIf(coverPicture.getAlbumId() != null,
                ErrorCode.PARAMS_ERROR, "该图片已属于其他相册");

        // 转换实体
        Album album = albumStructMapper.toAdd(albumAddRequest);
        // 设置相册 id 为封面图 id
        album.setId(coverId);
        // 填充默认值
        album.setPicCount(1); // 初始图片数量为 1（封面图）
        album.setUserId(loginUser.getId());

        // 写入数据库
        boolean result = save(album);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建相册失败");

        // 更新封面图片的 albumType 为 2（主图），并关联相册
        Picture updatePicture = new Picture();
        updatePicture.setId(coverId);
        updatePicture.setAlbumType(2); // 设置为主图
        updatePicture.setAlbumId(coverId); // 相册 id 就是图片 id
        pictureMapper.updateById(updatePicture);

        return album.getId();
    }

    @Override
    public void editAlbum(AlbumEditRequest albumEditRequest, User loginUser) {
        Long id = albumEditRequest.getId();
        // 判断是否存在
        Album oldAlbum = getById(id);
        Throw.throwIf(oldAlbum == null, ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        // 仅本人或管理员可编辑
        Throw.throwIf(!oldAlbum.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR);
        // 转换实体
        Album album = albumStructMapper.toEntity(albumEditRequest);
        // 设置编辑时间
        album.setEditTime(new Date());
        // 操作数据库
        boolean result = updateById(album);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑相册失败");

    }

    @Override
    public void deleteAlbum(Long id, User loginUser) {
        // 判断是否存在
        Album album = getById(id);
        Throw.throwIf(album == null, ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        // 仅本人或管理员可删除
        Throw.throwIf(!album.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR);


        // 解除相册内图片的关联关系
        pictureMapper.update(null,
                new LambdaUpdateWrapper<Picture>()
                        .eq(Picture::getAlbumId, id)
                        .set(Picture::getAlbumId, null)
                        .set(Picture::getAlbumType, AlbumTypeEnum.Not.getValue())
        );
        // 操作数据库
        boolean result = removeById(id);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除相册失败");
    }
    @Override
    public AlbumVO getAlbumVO(Album album) {
        if (album == null) {
            return null;
        }
        return albumStructMapper.toVo(album);
    }

    @Override
    public void addPicturesToAlbum(AlbumPictureAddRequest request, User loginUser) {
        Long albumId = request.getAlbumId();
        List<Long> pictureIds = request.getPictureIds();

        Throw.throwIf(CollUtil.isEmpty(pictureIds), ErrorCode.PARAMS_ERROR, "图片 ID 列表不能为空");

        // 检查相册是否存在且属于当前用户
        Album album = getById(albumId);
        Throw.throwIf(album == null, ErrorCode.NOT_FOUND_ERROR, "相册不存在");
        Throw.throwIf(!album.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR);

        // 检查这些图片是否已经属于其他相册
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Picture::getId, pictureIds);
        queryWrapper.ne(Picture::getAlbumType, 0);
        List<Picture> existedPictures = pictureMapper.selectList(queryWrapper);
        Throw.throwIf(CollUtil.isNotEmpty(existedPictures), ErrorCode.PARAMS_ERROR, "部分图片已属于其他相册");
        // 更新图片的相册信息
        for (Long pictureId : pictureIds) {
            Picture picture = pictureMapper.selectById(pictureId);
            Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在：" + pictureId);

            // 检查图片权限
            Throw.throwIf(!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH_ERROR, "无权限操作图片：" + pictureId);

            Picture updatePicture = new Picture();
            updatePicture.setId(pictureId);
            updatePicture.setAlbumType(1); // 设置为从图
            updatePicture.setAlbumId(albumId);
            pictureMapper.updateById(updatePicture);
        }
// 优化方式：直接增量更新（减少一次查询）
        lambdaUpdate()
                .eq(Album::getId, albumId)
                .setSql("picCount = picCount + " + pictureIds.size())
                .update();
    }


}
