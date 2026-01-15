package com.domye.picture.helper.impl;

import com.domye.picture.helper.Cache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Redis缓存实现类
 * 提供基于Redis的缓存操作，包括基础缓存操作、Hash操作、HyperLogLog操作、ZSet操作等
 */
@Slf4j
@Component
public class RedisCache implements Cache {

    /** Redis模板对象，用于执行Redis操作 */
    private final RedisTemplate<Object, Object> redisTemplate;

    /**
     * 构造函数
     * @param redisTemplate Redis模板对象
     */
    public RedisCache(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据键获取缓存值
     * @param key 缓存键
     * @return 缓存值，不存在返回null
     */
    @Override
    public Object get(Object key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 根据键获取字符串类型的缓存值
     * @param key 缓存键
     * @return 字符串类型的缓存值，发生异常时返回null
     */
    @Override
    public String getString(Object key) {
        try {
            return String.valueOf(redisTemplate.opsForValue().get(key));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 批量获取多个键对应的值
     * @param keys 键的集合
     * @return 值的列表
     */
    @SuppressWarnings("unchecked")
    @Override
    public List multiGet(Collection keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 批量设置多个键值对
     * @param map 键值对映射
     */
    @SuppressWarnings("unchecked")
    @Override
    public void multiSet(Map map) {
        redisTemplate.opsForValue().multiSet(map);
    }

    /**
     * 批量删除多个键
     * @param keys 键的集合
     */
    @SuppressWarnings("unchecked")
    @Override
    public void multiDel(Collection keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 设置键值对，无过期时间
     * @param key   缓存键
     * @param value 缓存值
     */
    @Override
    public void put(Object key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置键值对，指定过期时间（秒）
     * @param key   缓存键
     * @param value 缓存值
     * @param exp   过期时间，单位为秒
     */
    @Override
    public void put(Object key, Object value, Long exp) {
        put(key, value, exp, TimeUnit.SECONDS);
    }

    /**
     * 设置键值对，指定过期时间和时间单位
     * @param key      缓存键
     * @param value    缓存值
     * @param exp      过期时间
     * @param timeUnit 时间单位
     */
    @Override
    public void put(Object key, Object value, Long exp, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, exp, timeUnit);
    }

    /**
     * 删除指定键
     * @param key 要删除的键
     * @return 是否删除成功
     */
    @Override
    public Boolean remove(Object key) {
        return redisTemplate.delete(key);
    }

    /**
     * 模糊删除键
     * 删除所有以指定前缀开头的键
     * @param key 键的前缀，将删除所有以该前缀开头的键
     */
    @Override
    public void vagueDel(Object key) {
        Set<Object> keys = redisTemplate.keys(key + "*");
        redisTemplate.delete(Objects.requireNonNull(keys));
    }

    /**
     * 清空所有缓存
     * 注意：此操作会删除Redis中的所有键，请谨慎使用
     */
    @SuppressWarnings("unchecked")
    @Override
    public void clear() {
        Set keys = redisTemplate.keys("*");
        redisTemplate.delete(Objects.requireNonNull(keys));
    }

    /**
     * 向Hash结构中添加键值对
     * @param key       Hash的键
     * @param hashKey   Hash中的字段键
     * @param hashValue Hash中的字段值
     */
    @Override
    public void putHash(Object key, Object hashKey, Object hashValue) {
        redisTemplate.opsForHash().put(key, hashKey, hashValue);
    }

    /**
     * 批量向Hash结构中添加键值对
     * @param key Hash的键
     * @param map 包含多个字段键值对的Map
     */
    @SuppressWarnings("unchecked")
    @Override
    public void putAllHash(Object key, Map map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 获取Hash结构中指定字段的值
     * @param key     Hash的键
     * @param hashKey Hash中的字段键
     * @return 字段对应的值
     */
    @Override
    public Object getHash(Object key, Object hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取Hash结构中的所有字段键值对
     * @param key Hash的键
     * @return 包含所有字段键值对的Map
     */
    @Override
    public Map<Object, Object> getHash(Object key) {
        return this.redisTemplate.opsForHash().entries(key);
    }

    /**
     * 检查键是否存在
     * @param key 要检查的键
     * @return 如果键存在且值不为null，返回true；否则返回false
     */
    @Override
    public boolean hasKey(Object key) {
        return this.redisTemplate.opsForValue().get(key) != null;
    }


    /**
     * 获取符合指定模式的键列表
     * 使用SCAN命令遍历Redis键，避免KEYS命令可能导致的阻塞问题
     * @param pattern 匹配模式，如"user:*"
     * @return 符合模式的键列表
     */
    @Override
    public List<String> keys(String pattern) {
        List<String> keys = new ArrayList<>();
        this.scan(pattern, item -> {
            // 将字节数组转换为字符串
            String key = new String(item, StandardCharsets.UTF_8);
            keys.add(key);
        });
        return keys;
    }

    /**
     * 模糊搜索键
     * 使用SCAN命令查找匹配指定模式的所有键
     * @param pattern 匹配模式，如"user:*"
     * @return 包含所有匹配键的Set集合
     */
    @Override
    public Set<String> fuzzySearchKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions().match(pattern).build();
        try (Cursor<byte[]> cursor = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().scan(scanOptions)) {
            while (cursor.hasNext()) {
                byte[] next = cursor.next();
                String key = new String(next, StandardCharsets.UTF_8);
                keys.add(key);
            }
        }

        return keys;
    }

    /**
     * Redis SCAN命令的实现
     * 用于遍历Redis中的键，避免KEYS命令导致的阻塞问题
     * @param pattern  键的匹配模式
     * @param consumer 对每个匹配到的键执行的操作
     */
    private void scan(String pattern, Consumer<byte[]> consumer) {
        this.redisTemplate.execute((RedisConnection connection) -> {
            try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().count(Long.MAX_VALUE).match(pattern).build())) {
                cursor.forEachRemaining(consumer);
                return null;

            } catch (Exception e) {
                log.error("scan错误", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 使用HyperLogLog基数统计添加元素
     * HyperLogLog是一种概率数据结构，用于统计唯一元素的数量（基数）
     * @param key   HyperLogLog结构的键
     * @param value 要添加的元素
     * @return 如果HyperLogLog内部存储被修改，返回1；否则返回0
     */
    @Override
    public Long cumulative(Object key, Object value) {
        HyperLogLogOperations<Object, Object> operations = redisTemplate.opsForHyperLogLog();
        // add 方法对应 PFADD 命令
        return operations.add(key, value);

    }

    /**
     * 获取HyperLogLog统计的基数（唯一元素数量）
     * @param key HyperLogLog结构的键
     * @return 基数估计值
     */
    @Override
    public Long counter(Object key) {
        HyperLogLogOperations<Object, Object> operations = redisTemplate.opsForHyperLogLog();

        // size 方法对应 PFCOUNT 命令
        return operations.size(key);
    }

    /**
     * 批量获取多个HyperLogLog结构的基数
     * @param keys HyperLogLog结构的键集合
     * @return 包含每个HyperLogLog基数的列表
     */
    @Override
    public List multiCounter(Collection keys) {
        if (keys == null) {
            return new ArrayList();
        }
        List<Long> result = new ArrayList<>();
        for (Object key : keys) {
            result.add(counter(key));
        }
        return result;
    }

    /**
     * 合并多个HyperLogLog结构
     * 将多个HyperLogLog结构合并为一个，合并后的基数是所有HyperLogLog并集的基数估计
     * @param key 要合并的HyperLogLog结构的键
     * @return 合并后的HyperLogLog结构的基数
     */
    @Override
    public Long mergeCounter(Object... key) {
        HyperLogLogOperations<Object, Object> operations = redisTemplate.opsForHyperLogLog();
        // 计数器合并累加，union方法对应 PFMERGE 命令
        return operations.union(key[0], key);
    }

    /**
     * 原子性递增指定键的值，并设置过期时间
     * 如果键不存在，会自动创建并初始化为0
     * @param key      要递增的键
     * @param liveTime 过期时间，单位为秒
     * @return 递增前的值
     */
    @Override
    public Long incr(String key, long liveTime) {
        RedisAtomicLong entityIdCounter = new RedisAtomicLong(key, Objects.requireNonNull(redisTemplate.getConnectionFactory()));
        long increment = entityIdCounter.getAndIncrement();
        if (increment == 0 && liveTime > 0) {
            entityIdCounter.expire(liveTime, TimeUnit.SECONDS);
        }
        return increment;
    }

    /**
     * 原子性递增指定键的值
     * 如果键不存在，会自动创建并初始化为0
     * @param key 要递增的键
     * @return 递增前的值
     */
    @Override
    public Long incr(String key) {
        RedisAtomicLong entityIdCounter = new RedisAtomicLong(key, Objects.requireNonNull(redisTemplate.getConnectionFactory()));
        return entityIdCounter.getAndIncrement();
    }

    /**
     * 使用Sorted Set记录关键词并增加其分数
     * 对应Redis的ZINCRBY命令，对于一个Sorted Set，如果成员存在则将其分数增加指定值，不存在则创建一个分数为1的成员
     * @param sortedSetName Sorted Set的键名，无需预先创建，不存在会自动创建
     * @param keyword       关键词成员
     */
    @Override
    public void incrementScore(String sortedSetName, String keyword) {
        // 指向key名为sortedSetName的zset元素，分数增加1
        redisTemplate.opsForZSet().incrementScore(sortedSetName, keyword, 1);
    }

    /**
     * 使用Sorted Set记录关键词并增加指定分数
     * 对应Redis的ZINCRBY命令，对于一个Sorted Set，如果成员存在则将其分数增加指定值，不存在则创建一个分数为指定值的成员
     * @param sortedSetName Sorted Set的键名
     * @param keyword       关键词成员
     * @param score         要增加的分数值
     * @return
     */
    @Override
    public Double incrementScore(String sortedSetName, String keyword, Integer score) {
        return redisTemplate.opsForZSet().incrementScore(sortedSetName, keyword, score);
    }

    /**
     * 查询Sorted Set中指定范围的成员及其分数
     * 对应Redis的ZREVRANGE命令，返回的有序集合按分数降序排列（分数大的在前面）
     * @param sortedSetName Sorted Set的键名
     * @param start         查询范围开始位置（包含）
     * @param end           查询范围结束位置（包含）
     * @return 包含成员及其分数的集合，按分数降序排列
     */
    @Override
    public Set<ZSetOperations.TypedTuple<Object>> reverseRangeWithScores(String sortedSetName, Integer start, Integer end) {
        return this.redisTemplate.opsForZSet().reverseRangeWithScores(sortedSetName, start, end);
    }

    /**
     * 查询Sorted Set中前N个成员及其分数
     * 对应Redis的ZREVRANGE命令，返回的有序集合按分数降序排列（分数大的在前面）
     * @param sortedSetName Sorted Set的键名
     * @param count         要获取的成员数量
     * @return 包含成员及其分数的集合，按分数降序排列
     */
    @Override
    public Set<ZSetOperations.TypedTuple<Object>> reverseRangeWithScores(String sortedSetName, Integer count) {
        return this.redisTemplate.opsForZSet().reverseRangeWithScores(sortedSetName, 0, count);
    }

    /**
     * 向Sorted Set中添加成员
     * 对应Redis的ZADD命令
     * @param key   Sorted Set的键名
     * @param score 成员的分数，用于排序
     * @param value 成员的值
     * @return 添加成功返回true，否则返回false
     */
    @Override
    public boolean zAdd(String key, long score, String value) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 获取Sorted Set中指定分数区间的成员及其分数
     * 对应Redis的ZRANGEBYSCORE命令
     * @param key  Sorted Set的键名
     * @param from 起始分数（包含）
     * @param to   结束分数（包含）
     * @return 包含成员及其分数的集合
     */
    @Override
    public Set<ZSetOperations.TypedTuple<Object>> zRangeByScore(String key, int from, long to) {
        return redisTemplate.opsForZSet().rangeByScoreWithScores(key, from, to);
    }

    /**
     * 从Sorted Set中移除指定成员
     * 对应Redis的ZREM命令
     * @param key   Sorted Set的键名
     * @param value 要移除的成员值
     * @return 实际移除的成员数量
     */
    @Override
    public Long zRemove(String key, String... value) {
        return redisTemplate.opsForZSet().remove(key, value);
    }

    /**
     * 获取分布式锁
     * 使用SETNX命令实现分布式锁，如果获取失败会进行重试
     * @param key         锁的键名
     * @param lockTimeout 锁的超时时间（秒），防止死锁
     * @param checkCount  获取锁的重试次数
     * @return 获取锁成功返回true，失败返回false
     */
    @Override
    @SneakyThrows
    public Boolean lock(String key, Long lockTimeout, Integer checkCount) {
        while (checkCount-- > 0) {
            if (setValueIfAbsent(key, "X", lockTimeout)) {
                return true;
            }
            TimeUnit.MILLISECONDS.sleep(200L);
        }
        return false;
    }

    /**
     * 仅当键不存在时才设置值
     * 对应Redis的SETNX命令
     * @param key     键名
     * @param value   要设置的值
     * @param timeout 过期时间（秒）
     * @return 设置成功返回true，键已存在返回false
     */
    public Boolean setValueIfAbsent(String key, Object value, Long timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 设置键的过期时间
     * 对应Redis的EXPIRE命令
     * @param key      键名
     * @param time     过期时间长度
     * @param timeUnit 时间单位
     */
    @Override
    public void expireKey(String key, long time, TimeUnit timeUnit) {
        redisTemplate.expire(key, time, timeUnit);
    }

    /**
     * 原子性递增指定键的值
     * 对应Redis的INCR命令
     * @param key 键名
     * @return 递增后的值
     */
    public Long increment(String key) {
        return redisTemplate.boundValueOps(key).increment();
    }

    /**
     * 原子性递增指定键的值并设置过期时间
     * 对应Redis的INCR和EXPIRE命令
     * @param key      键名
     * @param time     过期时间长度
     * @param timeUnit 时间单位
     * @return 递增后的值
     */
    public Long increment(String key, long time, TimeUnit timeUnit) {
        BoundValueOperations<Object, Object> redisOper = redisTemplate.boundValueOps(key);
        Long cur = redisOper.increment();
        redisOper.expire(time, timeUnit);
        return cur;
    }

    /**
     * 原子性递减指定键的值
     * 对应Redis的DECR命令
     * @param key 键名
     * @return 递减后的值
     */
    public Long decrement(String key) {
        return redisTemplate.boundValueOps(key).decrement();
    }

    //-----------------------------------------------Set操作实现--------------------------------------------

    /**
     * 向Set集合中添加元素
     * 对应Redis的SADD命令
     * @param key    Set的键
     * @param values 要添加的元素
     * @return 添加成功的元素数量（不包括已存在的元素）
     */
    @Override
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 获取Set集合中的所有元素
     * 对应Redis的SMEMBERS命令
     * @param key Set的键
     * @return Set集合中的所有元素
     */
    @Override
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断元素是否存在于Set集合中
     * 对应Redis的SISMEMBER命令
     * @param key   Set的键
     * @param value 要判断的元素
     * @return 存在返回true，不存在返回false
     */
    @Override
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取Set集合的大小
     * 对应Redis的SCARD命令
     * @param key Set的键
     * @return Set集合的大小
     */
    @Override
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 从Set集合中移除指定元素
     * 对应Redis的SREM命令
     * @param key    Set的键
     * @param values 要移除的元素
     * @return 实际移除的元素数量
     */
    @Override
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 获取多个Set集合的交集
     * 对应Redis的SINTER命令
     * @param keys 多个Set的键
     * @return 交集结果
     */
    @Override
    public Set<Object> sIntersect(String... keys) {
        return redisTemplate.opsForSet().intersect(Arrays.asList(keys));
    }

    /**
     * 获取多个Set集合的并集
     * 对应Redis的SUNION命令
     * @param keys 多个Set的键
     * @return 并集结果
     */
    @Override
    public Set<Object> sUnion(String... keys) {
        return redisTemplate.opsForSet().union(Arrays.asList(keys));
    }

    /**
     * 获取Set集合的差集（第一个Set与其他Set的差集）
     * 对应Redis的SDIFF命令
     * @param key       第一个Set的键
     * @param otherKeys 其他Set的键
     * @return 差集结果
     */
    @Override
    public Set<Object> sDifference(String key, String... otherKeys) {
        return redisTemplate.opsForSet().difference(key, otherKeys);
    }

    /**
     * 执行Lua脚本
     * @param luaScript Lua脚本内容
     * @param keys      Redis键列表
     * @param args      参数列表
     * @param <T>       返回值类型
     * @return Lua脚本执行结果
     */
    public <T> T executeLuaScript(String luaScript, List<String> keys, List<String> args) {
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>(luaScript);
        redisScript.setResultType((Class<T>) Long.class);
        List<Object> keyObjects = new ArrayList<>(keys);
        List<Object> argObjects = new ArrayList<>(args);
        return redisTemplate.execute(redisScript, keyObjects, argObjects.toArray());
    }


}