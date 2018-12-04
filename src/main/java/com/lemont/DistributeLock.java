package com.lemont;

public interface DistributeLock {
    boolean tryLock();

    void lock() throws InterruptedException;

    void unLock();
}
