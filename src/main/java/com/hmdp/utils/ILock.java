package com.hmdp.utils;

public interface ILock {

    boolean isLocked(Long timeoutSeconds);

    void unlock();
}
