package com.domye.picture.helper.impl;

import com.domye.picture.exception.BusinessException;
import com.domye.picture.exception.ErrorCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
@Slf4j
public class LockService {

    @Autowired
    private RedissonClient redissonClient;

    public <T> T lock(String key, SupplierThrow<T> supplier) throws Throwable {
        RLock lock = redissonClient.getLock(key);
        lock.lock();
        log.info("lock:{}", key);
        try {
            return supplier.get();
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                log.info("lock release:{}", key);
                lock.unlock();
            }
        }
    }

    public <T> T executeWithLockThrows(String key, int waitTime, TimeUnit unit, SupplierThrow<T> supplier) throws Throwable {
        RLock lock = redissonClient.getLock(key);
        boolean lockSuccess = lock.tryLock(waitTime, unit);
        if (!lockSuccess) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        try {
            return supplier.get();
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @SneakyThrows
    public <T> T executeWithLock(String key, int waitTime, TimeUnit unit, Supplier<T> supplier) {
        return executeWithLockThrows(key, waitTime, unit, supplier::get);
    }

    public <T> T executeWithLock(String key, Supplier<T> supplier) {
        return executeWithLock(key, -1, TimeUnit.MILLISECONDS, supplier);
    }

    @FunctionalInterface
    public interface SupplierThrow<T> {

        T get() throws Throwable;
    }

}
