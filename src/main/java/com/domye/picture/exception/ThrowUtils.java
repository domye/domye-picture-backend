package com.domye.picture.exception;

public class ThrowUtils {
    /**
     *
     * @param condition
     * @param runtimeException
     */
    public static void throwIf(boolean condition,RuntimeException runtimeException){
        if(condition){
            throw runtimeException;
        }
    }

    /**
     *
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition,ErrorCode errorCode){
        throwIf(condition,new BusinessException(errorCode));
    }

    public static void throwIf(boolean condition,ErrorCode errorCode,String message){
        throwIf(condition,new BusinessException(errorCode,message));
    }
}
