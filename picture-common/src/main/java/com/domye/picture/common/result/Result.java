package com.domye.picture.common.result;

import com.domye.picture.common.exception.ErrorCode;
/**
 * 响应工具类
 * 提供统一的响应构建方法
 */
public class Result {

    /**
     * 成功
     * @param data 数据
     * @param <T>  数据类型
     * @return 响应
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @return 响应
     */

    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 失败
     * @param code    错误码
     * @param message 错误信息
     * @return 响应
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 失败
     * @param errorCode 错误码
     * @return 响应
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }


    // ==================== 增强错误响应方法（带时间戳和路径） ====================

    /**
     * 创建参数错误响应（带时间戳和路径）
     *
     * @param code      错误码
     * @param message   错误信息
     * @param path      请求路径
     * @return 错误响应
     */
    public static <T> ErrorResponse<T> paramError(int code, String message, String path) {
        return new ErrorResponse<>(
            code,
            message,
            ErrorResponse.currentTimestamp(),
            path,
            ErrorResponse.ErrorType.PARAM_ERROR
        );
    }

    /**
     * 创建认证授权错误响应（带时间戳和路径）
     *
     * @param code      错误码
     * @param message   错误信息
     * @param path      请求路径
     * @return 错误响应
     */
    public static <T> ErrorResponse<T> authError(int code, String message, String path) {
        return new ErrorResponse<>(
            code,
            message,
            ErrorResponse.currentTimestamp(),
            path,
            ErrorResponse.ErrorType.AUTH_ERROR
        );
    }

    /**
     * 创建业务错误响应（带时间戳和路径）
     *
     * @param code      错误码
     * @param message   错误信息
     * @param path      请求路径
     * @return 错误响应
     */
    public static <T> ErrorResponse<T> bizError(int code, String message, String path) {
        return new ErrorResponse<>(
            code,
            message,
            ErrorResponse.currentTimestamp(),
            path,
            ErrorResponse.ErrorType.BIZ_ERROR
        );
    }

    /**
     * 创建系统错误响应（带时间戳和路径）
     *
     * @param code      错误码
     * @param message   错误信息
     * @param path      请求路径
     * @return 错误响应
     */
    public static <T> ErrorResponse<T> systemError(int code, String message, String path) {
        return new ErrorResponse<>(
            code,
            message,
            ErrorResponse.currentTimestamp(),
            path,
            ErrorResponse.ErrorType.SYSTEM_ERROR
        );
    }

    /**
     * 创建系统错误响应（使用 ErrorCode）
     *
     * @param errorCode 错误码枚举
     * @param path      请求路径
     * @return 错误响应
     */
    public static <T> ErrorResponse<T> systemError(ErrorCode errorCode, String path) {
        return new ErrorResponse<>(
            errorCode,
            ErrorResponse.currentTimestamp(),
            path,
            ErrorResponse.ErrorType.SYSTEM_ERROR
        );
    }
}
