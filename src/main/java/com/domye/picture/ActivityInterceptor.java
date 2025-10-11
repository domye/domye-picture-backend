package com.domye.picture;

import com.domye.picture.service.rank.RankService;
import com.domye.picture.service.rank.model.dto.UserActivityScoreAddRequest;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class ActivityInterceptor implements HandlerInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private RankService rankService;

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        try {
            User user = userService.getLoginUser(request);
            if (user == null) {
                return;
            }

            String uri = request.getRequestURI();
            if (uri.startsWith("/picture/")) {
                // 处理图片详情页
                String pictureId = uri.substring("/picture/".length());
                if (StringUtils.isNumeric(pictureId)) {
                    UserActivityScoreAddRequest addRequest = new UserActivityScoreAddRequest();
                    addRequest.setPictureId(Long.parseLong(pictureId));  // 修改这里
                    rankService.addActivityScore(user, addRequest);
                    log.info("更新图片访问活跃度: userId={}, pictureId={}", user.getId(), pictureId);
                    return;
                }
            }

            // 处理其他页面
            UserActivityScoreAddRequest addRequest = new UserActivityScoreAddRequest();
            addRequest.setPath(uri);
            rankService.addActivityScore(user, addRequest);

        } catch (Exception e) {
            log.error("更新用户活跃度失败", e);
        }
    }
}
