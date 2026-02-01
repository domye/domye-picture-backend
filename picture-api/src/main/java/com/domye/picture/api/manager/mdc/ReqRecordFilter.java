package com.domye.picture.api.manager.mdc;

import cn.hutool.core.date.StopWatch;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@WebFilter(urlPatterns = "/*", filterName = "reqRecordFilter", asyncSupported = true)
@Order(1)
public class ReqRecordFilter implements Filter {
    private static final String GLOBAL_TRACE_ID_HEADER = "g-trace-id";
    private static final Logger REQ_LOG = LoggerFactory.getLogger("req");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("ReqRecordFilter init...");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        long start = System.currentTimeMillis();
        StopWatch stopWatch = new StopWatch("请求耗时");
        String traceId = null;

        try {
            // 静态资源直接放行
            if (isStaticURI(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            stopWatch.start("请求参数构建");
            // 包装请求以便重复读取
            request = new ContentCachingRequestWrapper(request);

            // 添加traceId到MDC
            MdcUtil.addTraceId();
            traceId = MdcUtil.getTraceId();

            // 将traceId添加到响应头中
            response.setHeader(GLOBAL_TRACE_ID_HEADER, traceId);

            // 记录请求基本信息
            logRequestInfo(request, traceId);
            stopWatch.stop();

            stopWatch.start("业务执行");
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Request error. traceId:{}", traceId, e);
            throw e;
        } finally {
            if (stopWatch.isRunning()) {
                stopWatch.stop();
            }

            stopWatch.start("输出请求日志");
            // 记录请求耗时和详细信息
            long costTime = System.currentTimeMillis() - start;
            buildRequestLog(request, costTime);

            // 清理MDC
            MdcUtil.clear();
            stopWatch.stop();

            if (!isStaticURI(request)) {
                log.info("{} - cost:\n{}", request.getRequestURI(), stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
            }
        }
    }

    @Override
    public void destroy() {
        log.info("ReqRecordFilter destroy...");
    }

    private boolean isStaticURI(HttpServletRequest request) {
        if (request == null) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri.endsWith("css") || uri.endsWith("js") || uri.endsWith("png")
                || uri.endsWith("ico") || uri.endsWith("gif") || uri.endsWith("svg")
                || uri.endsWith("min.js.map") || uri.endsWith("min.css.map");
    }

    private void logRequestInfo(HttpServletRequest request, String traceId) {
        REQ_LOG.info("Request start. traceId:{}, method:{}, uri:{}, clientIp:{}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request));
    }

    private void buildRequestLog(HttpServletRequest request, long costTime) {
        StringBuilder msg = new StringBuilder();
        msg.append("method=").append(request.getMethod()).append("; ")
                .append("uri=").append(request.getRequestURI()).append("; ")
                .append("clientIp=").append(getClientIp(request)).append("; ")
                .append("userAgent=").append(request.getHeader("User-Agent")).append("; ")
                .append("cost=").append(costTime).append("ms");

        REQ_LOG.info("{}", msg);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
