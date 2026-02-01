package com.domye.picture.api.manager.auth;


import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.domye.picture.api.manager.auth.model.SpaceUserPermissionConstant;
import com.domye.picture.api.service.picture.PictureService;
import com.domye.picture.api.service.space.SpaceService;
import com.domye.picture.api.service.space.SpaceUserService;
import com.domye.picture.api.service.user.UserService;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.picture.entity.Picture;
import com.domye.picture.model.space.entity.Space;
import com.domye.picture.model.space.entity.SpaceUser;
import com.domye.picture.model.space.enums.SpaceRoleEnum;
import com.domye.picture.model.space.enums.SpaceTypeEnum;
import com.domye.picture.model.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static com.domye.picture.api.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    // 默认是 /api
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 1. 校验登录类型：如果 loginType 不是 "space"，直接返回空权限列表
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }

        // 2. 获取当前登录用户信息
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);

        Throw.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR, "用户未登录");


        // 3. 管理员权限处理：如果当前用户为管理员，直接返回管理员权限列表
        if (userService.isAdmin(loginUser)) {
            return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        }

        // 4. 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();

        // 5. 检查上下文字段是否为空
        if (isAllFieldsNull(authContext)) {
            // 如果上下文中所有字段均为空（如没有空间或图片信息），视为公共图库操作
            // 公共图库操作，返回仅查看权限
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }

        Long userId = loginUser.getId();

        // 6. 从上下文中优先获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        // 7. 通过 spaceUserId 获取空间用户信息
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);

            Throw.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");

            // 校验当前登录用户是否属于该空间
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }

            // 根据登录用户在该空间的角色，返回相应的权限码列表
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }

        // 8. 通过 spaceId 或 pictureId 获取空间或图片信息
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果没有 spaceId，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            if (pictureId == null) {
                // 如果 pictureId 和 spaceId 均为空，默认视为仅查看权限
                return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
            }

            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();

            Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");

            spaceId = picture.getSpaceId();
            // 公共图库处理
            if (spaceId == null) {
                if (picture.getUserId().equals(userId)) {
                    // 如果图片是当前用户上传的，返回管理员权限
                    return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
                } else {
                    // 如果图片不是当前用户上传的，返回仅允许查看的权限码
                    return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }

        // 9. 获取 Space 对象并判断空间类型
        Space space = spaceService.getById(spaceId);

        Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");

        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间：仅空间所有者和管理员有权限
            if (space.getUserId().equals(userId)) {
                return spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间：查询登录用户在该空间的角色
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            // 返回对应的权限码列表
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }


    /**
     * 本项目中不使用。返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 获取请求参数
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            // 获取到请求路径的业务前缀，/api/picture/aaa?a=1
            String requestURI = request.getRequestURI();
            // 先替换掉上下文，剩下的就是前缀
            String partURI = requestURI.replace(contextPath + "/", "");
            // 获取前缀的第一个斜杠前的字符串
            String moduleName = StrUtil.subBefore(partURI, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }

    /**
     * 判断对象的所有字段是否为空
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }
}
