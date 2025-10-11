package com.domye.picture.config;

import com.domye.picture.ActivityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private ActivityInterceptor activityInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(activityInterceptor)
                .addPathPatterns("/**")  // 拦截所有路径
                .excludePathPatterns(
                        "/static/**",        // 排除静态资源
                        "/error",           // 排除错误页面
                        "/user/login",      // 排除登录接口
                        "/user/register"    // 排除注册接口
                )
                .order(1);  // 设置执行顺序
    }
}
