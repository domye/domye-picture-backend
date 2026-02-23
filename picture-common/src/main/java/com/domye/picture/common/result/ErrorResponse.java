package com.domye.picture.common.result;

import com.domye.picture.common.exception.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.Instant;

/**
 * 统一错误响应结构
 * 扩展 BaseResponse，增加时间戳、请求路径和错误类型信息
 * 用于异常情况下的标准化响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorResponse<T> extends BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 异常发生时间戳 (ISO 8601 格式)
     */
    private String timestamp;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 错误类型分类
     * PARAM_ERROR - 参数校验错误
     * AUTH_ERROR - 认证授权错误
     * BIZ_ERROR - 业务逻辑错误
     * SYSTEM_ERROR - 系统内部错误
     */
    private String errorType;

    /**
     * 构造错误响应
     *
     * @param code      错误码
     * @param message   错误消息
     * @param timestamp 时间戳
     * @param path      请求路径
     * @param errorType 错误类型
     */
    public ErrorResponse(int code, String message, String timestamp, String path, String errorType) {
        super(code, null, message);
        this.timestamp = timestamp;
        this.path = path;
        this.errorType = errorType;
    }

    /**
     * 使用 ErrorCode 构造错误响应
     *
     * @param errorCode 错误码枚举
     * @param timestamp 时间戳
     * @param path      请求路径
     * @param errorType 错误类型
     */
    public ErrorResponse(ErrorCode errorCode, String timestamp, String path, String errorType) {
        super(errorCode.getCode(), null, errorCode.getMessage());
        this.timestamp = timestamp;
        this.path = path;
        this.errorType = errorType;
    }

    /**
     * 错误类型常量
     */
    public static class ErrorType {
        /** 参数校验错误 */
        public static final String PARAM_ERROR = "PARAM_ERROR";
        /** 认证授权错误 */
        public static final String AUTH_ERROR = "AUTH_ERROR";
        /** 业务逻辑错误 */
        public static final String BIZ_ERROR = "BIZ_ERROR";
        /** 系统内部错误 */
        public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    }

    /**
     * 获取当前时间戳 (ISO 8601 格式)
     */
    public static String currentTimestamp() {
        return Instant.now().toString();
    }
}
