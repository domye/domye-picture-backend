package com.domye.picture.manager.mdc;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 1. 请求参数日志输出过滤器
 * 2. 判断用户是否登录
 * @author YiHui
 * @date 2022/7/6
 */
@Slf4j
@WebFilter(urlPatterns = "/*", filterName = "reqRecordFilter", asyncSupported = true)
public class ReqRecordFilter implements Filter {
    /**
     * 返回给前端的traceId，用于日志追踪
     */
    private static final String GLOBAL_TRACE_ID_HEADER = "g-trace-id";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            // 添加traceId到MDC
            MdcUtil.addTraceId();

            // 将traceId添加到响应头中，方便前端追踪
            response.setHeader(GLOBAL_TRACE_ID_HEADER, MdcUtil.getTraceId());

            // 继续执行后续过滤器
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理MDC
            MdcUtil.clear();
        }
    }

}