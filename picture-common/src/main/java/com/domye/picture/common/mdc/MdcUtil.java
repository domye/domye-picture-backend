package com.domye.picture.common.mdc;

import org.slf4j.MDC;

/**
 * MDC (Mapped Diagnostic Context) 工具类
 * 用于在日志中记录链路追踪信息，支持分布式追踪
 */
public class MdcUtil {
    /**
     * MDC 中存储 TraceId 的 key
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * 向 MDC 添加键值对
     */
    public static void add(String key, String val) {
        if (key != null && val != null) {
            MDC.put(key, val);
        }
    }

    /**
     * 生成并设置 TraceId
     */
    public static void addTraceId() {
        String traceId = SelfTraceIdGenerator.generate();
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前 TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 获取 MDC 中指定 key 的值
     */
    public static String get(String key) {
        return MDC.get(key);
    }

    /**
     * 重置 MDC，保留 TraceId
     * 用于清除其他 MDC 信息但保持链路追踪
     */
    public static void reset() {
        String traceId = MDC.get(TRACE_ID_KEY);
        MDC.clear();
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 清空 MDC 所有信息
     * 应在请求结束时调用，防止线程池污染
     */
    public static void clear() {
        MDC.clear();
    }

}
