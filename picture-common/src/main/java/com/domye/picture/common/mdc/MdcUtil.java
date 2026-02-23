package com.domye.picture.common.mdc;

import cn.hutool.core.util.StrUtil;
import org.slf4j.MDC;

import java.util.Map;

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
     * 分布式追踪请求头名称
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

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
     * 设置指定的 TraceId（用于分布式追踪）
     * 如果传入的 traceId 为空，则生成新的
     */
    public static void setTraceId(String traceId) {
        if (StrUtil.isNotBlank(traceId)) {
            MDC.put(TRACE_ID_KEY, traceId);
        } else {
            addTraceId();
        }
    }

    /**
     * 仅当 MDC 中不存在该 key 时才设置值
     */
    public static void putIfAbsent(String key, String val) {
        if (key != null && val != null && MDC.get(key) == null) {
            MDC.put(key, val);
        }
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
     * 移除 MDC 中指定 key
     */
    public static void remove(String key) {
        MDC.remove(key);
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

    /**
     * 获取当前 MDC 的所有上下文信息（用于异步线程传递）
     */
    public static Map<String, String> getCopyOfContextMap() {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return contextMap != null ? contextMap : Map.of();
    }

    /**
     * 设置 MDC 上下文（用于异步线程传递）
     */
    public static void setContextMap(Map<String, String> contextMap) {
        if (contextMap != null && !contextMap.isEmpty()) {
            MDC.setContextMap(contextMap);
        }
    }
}
