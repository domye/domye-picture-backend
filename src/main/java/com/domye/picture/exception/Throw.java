package com.domye.picture.exception;

public class Throw {
    /**
     * 条件判断抛出异常
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /**
     * 条件判断抛出异常
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 条件判断抛出异常
     * @param condition
     * @param errorCode
     * @param message
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }

    /**
     * 直接抛出异常
     * @param runtimeException
     */
    public static void throwEx(RuntimeException runtimeException) {
        throw runtimeException;
    }

    /**
     * 直接抛出异常
     * @param errorCode
     */
    public static void throwEx(ErrorCode errorCode) {
        throw new BusinessException(errorCode);
    }

    /**
     * 直接抛出异常
     * @param errorCode
     * @param message
     */
    public static void throwEx(ErrorCode errorCode, String message) {
        throw new BusinessException(errorCode, message);
    }
}
