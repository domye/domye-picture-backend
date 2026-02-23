package com.domye.picture.common.constant;

public interface SpaceConstant {
    /**
     * 默认空间名称
     */
    String DEFAULT_SPACE_NAME = "默认空间";
    
    /**
     * 无权限创建指定级别的空间
     */
    String NO_PERMISSION_CREATE_SPACE = "无权限创建指定级别的空间";
    
    /**
     * 已存在该类型空间
     */
    String SPACE_ALREADY_EXISTS = "已存在该类型空间";
    
    /**
     * 空间创建锁前缀
     */
    String SPACE_CREATE_LOCK_PREFIX = "space:create:lock:";
    
    /**
     * 锁等待时间 (秒)
     */
    long LOCK_WAIT_TIME = 10;
    
    /**
     * 锁租约时间 (秒)
     */
    long LOCK_LEASE_TIME = 30;
}
