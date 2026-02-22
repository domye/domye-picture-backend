package com.domye.picture.auth;


import cn.hutool.core.util.StrUtil;
import com.domye.picture.model.dto.rank.UserActivityScoreAddRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.service.api.rank.RankService;
import com.domye.picture.service.api.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityInterceptor implements HandlerInterceptor {


    final UserService userService;

    final RankService rankService;

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
            if (StrUtil.isNumeric(pictureId)) {
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

