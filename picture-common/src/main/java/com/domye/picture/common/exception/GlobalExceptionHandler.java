package com.domye.picture.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.domye.picture.common.mdc.MdcUtil;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.ErrorResponse;
import com.domye.picture.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


/**
 * 全局异常处理器
 * 负责捕获并统一处理系统中抛出的各类异常
 * 提供统一的错误响应格式，包含错误码、错误消息、时间戳和请求路径
 * 使用 @Order 确保此处理器具有最高优先级
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 获取请求路径信息
     */
    private String getRequestPath(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }

    // ==================== 业务异常处理 (BIZ_ERROR) ====================

    /**
     * 业务异常处理
     * 处理自定义的 BusinessException，用于业务逻辑错误
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public BaseResponse<?> businessExceptionHandler(BusinessException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("[{}] BusinessException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            e.getMessage(),
            e);
        return Result.bizError(e.getCode(), e.getMessage(), path);
    }

    // ==================== 参数校验异常处理 (PARAM_ERROR) ====================

    /**
     * 处理 @Valid 注解校验失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数校验失败");
        log.error("[{}] MethodArgumentNotValidException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            errorMessage,
            e);
        return Result.paramError(ErrorCode.PARAMS_ERROR.getCode(), errorMessage, path);
    }

    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> bindExceptionHandler(BindException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数绑定失败");
        log.error("[{}] BindException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            errorMessage,
            e);
        return Result.paramError(ErrorCode.PARAMS_ERROR.getCode(), errorMessage, path);
    }

    /**
     * 处理缺少必需请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> missingServletRequestParameterExceptionHandler(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        String errorMessage = "缺少必需参数: " + e.getParameterName();
        log.error("[{}] MissingServletRequestParameterException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            errorMessage,
            e);
        return Result.paramError(ErrorCode.PARAMS_ERROR.getCode(), errorMessage, path);
    }

    /**
     * 处理参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> methodArgumentTypeMismatchExceptionHandler(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        String errorMessage = "参数类型错误: " + e.getName() + " 应为 " + 
            (e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型");
        log.error("[{}] MethodArgumentTypeMismatchException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            errorMessage,
            e);
        return Result.paramError(ErrorCode.PARAMS_ERROR.getCode(), errorMessage, path);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public BaseResponse<?> illegalArgumentExceptionHandler(
            IllegalArgumentException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("[{}] IllegalArgumentException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            e.getMessage(),
            e);
        return Result.paramError(ErrorCode.PARAMS_ERROR.getCode(), 
            e.getMessage() != null ? e.getMessage() : "参数错误", path);
    }

    // ==================== 认证授权异常处理 (AUTH_ERROR) ====================

    /**
     * 处理未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public BaseResponse<?> notLoginExceptionHandler(NotLoginException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("[{}] NotLoginException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            e.getMessage(),
            e);
        return Result.authError(ErrorCode.NOT_LOGIN_ERROR.getCode(), "请先登录", path);
    }

    /**
     * 处理无权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public BaseResponse<?> notPermissionExceptionHandler(
            NotPermissionException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("[{}] NotPermissionException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            e.getMessage(),
            e);
        return Result.authError(ErrorCode.NO_AUTH_ERROR.getCode(), "权限不足", path);
    }

    // ==================== 资源未找到异常处理 (NOT_FOUND) ====================

    /**
     * 处理请求方法不支持异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public BaseResponse<?> httpRequestMethodNotSupportedExceptionHandler(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        String errorMessage = "不支持的请求方法: " + e.getMethod();
        log.error("[{}] HttpRequestMethodNotSupportedException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            errorMessage,
            e);
        return Result.paramError(ErrorCode.FORBIDDEN_ERROR.getCode(), errorMessage, path);
    }


    // ==================== 系统异常处理 (SYSTEM_ERROR) ====================

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> nullPointerExceptionHandler(
            NullPointerException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("[{}] NullPointerException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            e.getMessage(),
            e);
        return Result.systemError(ErrorCode.SYSTEM_ERROR.getCode(), "系统内部错误", path);
    }

    /**
     * 处理运行时异常（兜底处理）
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("[{}] RuntimeException - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            e.getMessage(),
            e);
        return Result.systemError(ErrorCode.SYSTEM_ERROR.getCode(), "系统内部错误", path);
    }

    /**
     * 处理所有未捕获的异常（最终兜底处理）
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BaseResponse<?> exceptionHandler(Exception e, HttpServletRequest request) {
        String path = request.getRequestURI();
        log.error("[{}] Exception - Path: {}, Message: {}",
            MdcUtil.getTraceId(),
            getRequestPath(request),
            e.getMessage(),
            e);
        return Result.systemError(ErrorCode.SYSTEM_ERROR.getCode(), "系统内部错误", path);
    }
}
