package com.tencent.devops.common.redis.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis distributed lock implementation
 */
public class JRedisLock {
    public static final int ONE_SECOND = 1000;
    public static final int DEFAULT_EXPIRY_TIME_MILLIS = Integer.getInteger("lock.expiry.millis", 60 * ONE_SECOND);
    public static final int DEFAULT_ACQUIRE_TIMEOUT_MILLIS = Integer.getInteger("lock.acquiry.millis", 10 * ONE_SECOND);
    public static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = Integer.getInteger("lock.acquiry.resolution.millis",
            100);
    private static final Lock NO_LOCK = new Lock(new UUID(0L, 0L), 0L);
    private static Logger logger = LoggerFactory.getLogger(JRedisLock.class);
    private final StringRedisTemplate redisTemplate;

    private final String lockKeyPath;

    private final int lockExpiryInMillis;
    private final int acquiryTimeoutInMillis;
    private final UUID lockUUID;
    private final String lockID;
    private Lock lock = null;

    /**
     * Detailed constructor with default acquire timeout 10000 msecs and lock
     * expiration of 60000 msecs.
     *
     * @param redisTemplate
     * @param lockKey       lock key (ex. account:1, ...)
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey) {
        this(redisTemplate, lockKey, DEFAULT_ACQUIRE_TIMEOUT_MILLIS, DEFAULT_EXPIRY_TIME_MILLIS);
    }


    /**
     * Detailed constructor with default lock expiration of 60000 msecs.
     *
     * @param redisTemplate
     * @param lockKey              lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis acquire timeout in miliseconds (default: 10000 msecs)
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey, int acquireTimeoutMillis) {
        this(redisTemplate, lockKey, acquireTimeoutMillis, DEFAULT_EXPIRY_TIME_MILLIS);
    }

    /**
     * Detailed constructor.
     *
     * @param redisTemplate
     * @param lockKey              lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis acquire timeout in miliseconds (default: 10000 msecs)
     * @param expiryTimeMillis     lock expiration in miliseconds (default: 60000 msecs)
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey, int acquireTimeoutMillis,
                      int expiryTimeMillis) {
        this(redisTemplate, lockKey, acquireTimeoutMillis, expiryTimeMillis, UUID.randomUUID());
    }

    /**
     * Detailed constructor.
     *
     * @param redisTemplate
     * @param lockKey              lock key (ex. account:1, ...)
     * @param acquireTimeoutMillis acquire timeout in miliseconds (default: 10000 msecs)
     * @param expiryTimeMillis     lock expiration in miliseconds (default: 60000 msecs)
     * @param uuid                 unique identification of this lock
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey, int acquireTimeoutMillis,
                      int expiryTimeMillis, UUID uuid) {
        this.redisTemplate = redisTemplate;
        this.lockKeyPath = lockKey;
        this.acquiryTimeoutInMillis = acquireTimeoutMillis;
        this.lockExpiryInMillis = expiryTimeMillis + 1;
        this.lockUUID = uuid;
        this.lockID = null;
    }

    /**
     * Detailed constructor.
     *
     * @param redisTemplate
     * @param lockKey              lock key (ex. account:1, ...)
     * @param expiryTimeMillis     lock expiration in miliseconds (default: 60000 msecs)
     * @param lockID               unique identification of this lock
     */
    public JRedisLock(StringRedisTemplate redisTemplate, String lockKey, int expiryTimeMillis, String lockID) {
        this.redisTemplate = redisTemplate;
        this.lockKeyPath = lockKey;
        this.acquiryTimeoutInMillis = 0;
        this.lockExpiryInMillis = expiryTimeMillis + 1;
        this.lockUUID = null;
        this.lockID = lockID;
    }

    /**
     * lock uuid
     *
     * @return lock uuid
     */
    public UUID getLockUUID() {
        return lockUUID;
    }

    /**
     * lock key path
     *
     * @return
     */
    public String getLockKeyPath() {
        return lockKeyPath;
    }

    /**
     * Acquire lock.
     *
     * @return true if lock is acquired, false acquire timeouted
     */
    public synchronized boolean acquire() {
        return acquire(redisTemplate);
    }

    /**
     * Acquire lock.
     *
     * @param redisTemplate
     * @return true if lock is acquired, false acquire timeouted
     */
    protected synchronized boolean acquire(StringRedisTemplate redisTemplate) {
        int timeout = acquiryTimeoutInMillis;
        while (timeout >= 0) {

            final Lock newLock = asLock(System.currentTimeMillis() + lockExpiryInMillis);
            if (redisTemplate.opsForValue().setIfAbsent(lockKeyPath, newLock.toString())) {
                this.lock = newLock;
                return true;
            }

            final String currentValueStr = redisTemplate.opsForValue().get(lockKeyPath);
            final Lock currentLock = Lock.fromString(currentValueStr);
            if (currentLock.isExpiredOrMine(lockUUID)) {
                String oldValueStr = redisTemplate.opsForValue().getAndSet(lockKeyPath, newLock.toString());
                if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                    this.lock = newLock;
                    return true;
                }
            }

            timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;

            try {
                Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
            } catch (InterruptedException e) {
                logger.error("Get lock[{}] failed!", this.getLockKeyPath(), e);
            }
        }

        logger.error("Get lock[{}] failed!", this.getLockKeyPath());
        return false;
    }


    /**
     * 获取不同客户锁（在一个线程加锁，在另一个线程解锁）
     *
     * @return true if lock is acquired
     */
    public synchronized boolean acquireDiffClientLock() {
        if (redisTemplate.opsForValue().setIfAbsent(lockKeyPath, lockID)) {
            redisTemplate.expire(lockKeyPath, lockExpiryInMillis, TimeUnit.MILLISECONDS);
            return true;
        }

        logger.debug("Get lock<{}, {}> failed!", this.getLockKeyPath(), lockID);
        return false;
    }

    /**
     * Acquired lock release.
     */
    public synchronized void releaseDiffClientLock() {
        final String currentValueStr = redisTemplate.opsForValue().get(lockKeyPath);
        if (lockID.equals(currentValueStr)) {
            redisTemplate.delete(lockKeyPath);
        }
    }

    /**
     * Acquired lock release.
     */
    public synchronized void release() {
        release(redisTemplate);
    }

    /**
     * Acquired lock release.
     *
     * @param redisTemplate
     */
    protected synchronized void release(StringRedisTemplate redisTemplate) {
        if (isLocked()) {
            redisTemplate.opsForValue().getOperations().delete(lockKeyPath);
            this.lock = null;
        }
    }

    /**
     * Check if owns the lock
     *
     * @return true if lock owned
     */
    public synchronized boolean isLocked() {
        return this.lock != null;
    }

    /**
     * Returns the expiry time of this lock
     *
     * @return the expiry time in millis (or null if not locked)
     */
    public synchronized long getLockExpiryTimeInMillis() {
        return this.lock.getExpiryTime();
    }

    private Lock asLock(long expires) {
        return new Lock(lockUUID, expires);
    }

    protected static class Lock {
        private UUID uuid;
        private long expiryTime;

        protected Lock(UUID uuid, long expiryTimeInMillis) {
            this.uuid = uuid;
            this.expiryTime = expiryTimeInMillis;
        }

        protected static Lock fromString(String text) {
            try {
                String[] parts = text.split(":");
                UUID theUUID = UUID.fromString(parts[0]);
                long theTime = Long.parseLong(parts[1]);
                return new Lock(theUUID, theTime);
            } catch (Exception any) {
                return NO_LOCK;
            }
        }

        public UUID getUUID() {
            return uuid;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        @Override
        public String toString() {
            return uuid.toString() + ":" + expiryTime;
        }

        boolean isExpired() {
            return getExpiryTime() < System.currentTimeMillis();
        }

        boolean isExpiredOrMine(UUID otherUUID) {
            return this.isExpired() || this.getUUID().equals(otherUUID);
        }
    }


}
