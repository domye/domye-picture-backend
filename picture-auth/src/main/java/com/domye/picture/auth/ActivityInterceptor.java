package com.domye.picture.auth;


import com.domye.picture.model.rank.dto.UserActivityScoreAddRequest;
import com.domye.picture.model.user.entity.User;
import com.domye.picture.service.rank.RankService;
import com.domye.picture.service.user.UserService;
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
            // 只处理 /api/picture/get/vo 接口的访问
            String uri = request.getRequestURI();
            if (!"/api/picture/get/vo".equals(uri)) {
                return;
            }

            User user = userService.getLoginUser(request);
            if (user == null) {
                return;
            }

            // 从请求参数中获取图片ID
            String pictureId = request.getParameter("id");
            if (StringUtils.isNumeric(pictureId)) {
                UserActivityScoreAddRequest addRequest = new UserActivityScoreAddRequest();
                addRequest.setPath(pictureId);
                rankService.addActivityScore(user, addRequest);
                log.info("更新图片访问活跃度: userId={}, pictureId={}", user.getId(), pictureId);
            }

        } catch (Exception e) {
            log.error("更新用户活跃度失败", e);
        }
    }
}

