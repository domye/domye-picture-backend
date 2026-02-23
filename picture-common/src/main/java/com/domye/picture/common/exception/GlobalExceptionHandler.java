package com.domye.picture.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.domye.picture.common.mdc.MdcUtil;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
// import jakarta.validation.ConstraintViolationException; // 需要 jakarta.validation 依赖
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 负责捕获并统一处理系统中抛出的各类异常
 * 使用 @Order 确保此处理器具有最高优先级
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler {


    // ==================== 业务异常处理 ====================

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e, HttpServletRequest request) {
        log.error("[{}] BusinessException - URL: {} {}, Message: {}", 
            MdcUtil.getTraceId(), 
            request.getMethod(), 
            request.getRequestURI(), 
            e.getMessage(), 
            e);
        return Result.error(e.getCode(), e.getMessage());
    }


    // ==================== 运行时异常处理 ====================

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e, HttpServletRequest request) {
        log.error("[{}] RuntimeException - URL: {} {}, Message: {}", 
            MdcUtil.getTraceId(), 
            request.getMethod(), 
            request.getRequestURI(), 
            e.getMessage(), 
            e);
        return Result.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }


    // ==================== 参数校验异常处理 ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数校验失败");
        log.error("[{}] MethodArgumentNotValidException - URL: {} {}, Message: {}", 
            MdcUtil.getTraceId(), 
            request.getMethod(), 
            request.getRequestURI(), 
            errorMessage, 
            e);
        return Result.error(ErrorCode.PARAMS_ERROR, errorMessage);
    }

    @ExceptionHandler(BindException.class)
    public BaseResponse<?> bindExceptionHandler(BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("参数绑定失败");
        log.error("[{}] BindException - URL: {} {}, Message: {}", 
            MdcUtil.getTraceId(), 
            request.getMethod(), 
            request.getRequestURI(), 
            errorMessage, 
            e);
        return Result.error(ErrorCode.PARAMS_ERROR, errorMessage);
    }

    // ConstraintViolationException 处理器已移除（需要 jakarta.validation 依赖）
    // 如需启用，请在 picture-common 中添加 jakarta.validation-api 依赖


    // ==================== 认证授权异常处理 ====================

    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e, HttpServletRequest request) {
        log.error("[{}] NotLoginException - URL: {} {}, Message: {}", 
            MdcUtil.getTraceId(), 
            request.getMethod(), 
            request.getRequestURI(), 
            e.getMessage(), 
            e);
        return Result.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }


    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e, HttpServletRequest request) {
        log.error("[{}] NotPermissionException - URL: {} {}, Message: {}", 
            MdcUtil.getTraceId(), 
            request.getMethod(), 
            request.getRequestURI(), 
            e.getMessage(), 
            e);
        return Result.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }
}
